############################################################
# Instanz und Web-Konsole
############################################################

# Learner Lab: keine eigenen IAM-Rollen, das vorhandene Profil wird nur referenziert
data "aws_iam_instance_profile" "lab" {
  name = "LabInstanceProfile"
}

data "aws_vpc" "default" {
  default = true
}

data "aws_ami" "al2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-2023.*-x86_64"]
  }
  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

# Nur HTTP. Kein SSH: Befehle auf der Instanz laufen ueber AWS Systems Manager (SSM).
resource "aws_security_group" "console" {
  name_prefix = "tictactest-"
  description = "TicTacTest Web-Konsole"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description = "HTTP: nginx liefert Web-Konsole (WebSocket) und /version.txt"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description = "ausgehend: Paketquellen, GitHub-Releases, SSM"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_instance" "console" {
  ami                    = data.aws_ami.al2023.id
  instance_type          = var.instance_type
  iam_instance_profile   = data.aws_iam_instance_profile.lab.name
  vpc_security_group_ids = [aws_security_group.console.id]

  metadata_options {
    http_tokens = "required" # IMDSv2
  }

  # Einrichtung der Instanz. Die Version der Applikation steht bewusst NICHT hier:
  # sonst wuerde jeder Release die Instanz neu erstellen (siehe docs/deployment.md).
  user_data = templatefile("${path.module}/user_data.sh.tftpl", {
    github_repo   = var.github_repo
    ttyd_version  = var.ttyd_version
    ttyd_sha256   = var.ttyd_sha256
    nginx_conf    = file("${path.module}/files/nginx.conf")
    console_unit  = file("${path.module}/files/tictactest-console.service")
    deploy_script = file("${path.module}/files/tictactest-deploy.sh")
  })
  # user_data laeuft nur beim ersten Start. Eine Aenderung hier hat ohne Neuerstellung keine Wirkung.
  user_data_replace_on_change = true

  lifecycle {
    # ein neueres Amazon-Linux-Image darf die laufende Instanz nicht ersetzen
    ignore_changes = [ami]
  }

  tags = {
    Name = "tictactest"
  }
}

# feste Adresse, ueberlebt Stopp und Start der Learner-Lab-Session
resource "aws_eip" "console" {
  domain   = "vpc"
  instance = aws_instance.console.id
}

############################################################
# Frontend: statische Webseite im S3-Bucket
############################################################

# Der Bucket wird ueber einen CloudFormation-Stack angelegt statt mit aws_s3_bucket: Der
# AWS-Provider liest nach dem Erstellen die Object-Lock-Konfiguration, und die
# Service Control Policy des Learner Labs verbietet s3:GetBucketObjectLockConfiguration.
# CloudFormation legt den Bucket an, ohne diese Einstellung zu lesen.
resource "aws_cloudformation_stack" "frontend" {
  name = "tictactest-frontend"

  # kein BucketName: CloudFormation vergibt einen weltweit eindeutigen Namen
  template_body = jsonencode({
    Resources = {
      Bucket = {
        Type = "AWS::S3::Bucket"
        Properties = {
          WebsiteConfiguration = {
            IndexDocument = "index.html"
          }
          PublicAccessBlockConfiguration = {
            BlockPublicAcls       = false
            BlockPublicPolicy     = false
            IgnorePublicAcls      = false
            RestrictPublicBuckets = false
          }
        }
      }
      Policy = {
        Type = "AWS::S3::BucketPolicy"
        Properties = {
          Bucket = { Ref = "Bucket" }
          PolicyDocument = {
            Version = "2012-10-17"
            Statement = [{
              Effect    = "Allow"
              Principal = "*"
              Action    = "s3:GetObject"
              Resource  = { "Fn::Join" = ["", [{ "Fn::GetAtt" = ["Bucket", "Arn"] }, "/*"]] }
            }]
          }
        }
      }
    }
    Outputs = {
      BucketName = { Value = { Ref = "Bucket" } }
      WebsiteURL = { Value = { "Fn::GetAtt" = ["Bucket", "WebsiteURL"] } }
    }
  })
}

# CloudFormation loescht keinen gefuellten Bucket. Vor dem Loeschen des Stacks (tofu destroy)
# wird er deshalb geleert. Benoetigt die AWS CLI; schlaegt es fehl, ist der Bucket von Hand zu leeren.
resource "terraform_data" "empty_bucket_on_destroy" {
  input = {
    bucket = aws_cloudformation_stack.frontend.outputs["BucketName"]
    region = var.region
  }

  provisioner "local-exec" {
    when       = destroy
    command    = "aws s3 rm s3://${self.input.bucket} --recursive --region ${self.input.region}"
    on_failure = continue
  }
}

# Nur die Infrastruktur kennt die Adresse der Instanz. Das Deployment ueberspringt diese
# Datei (aws s3 sync --exclude config.js), das Release-Archiv enthaelt sie nicht.
resource "aws_s3_object" "config" {
  bucket       = aws_cloudformation_stack.frontend.outputs["BucketName"]
  key          = "config.js"
  content_type = "application/javascript"
  content      = <<-EOT
    window.TICTACTEST_CONFIG = {
      consoleUrl: "http://${aws_eip.console.public_ip}",
      versionUrl: "http://${aws_eip.console.public_ip}/version.txt",
    };
  EOT
}
