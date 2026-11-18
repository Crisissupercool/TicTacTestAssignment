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

resource "aws_s3_bucket" "frontend" {
  bucket_prefix = "tictactest-frontend-" # Bucket-Namen sind weltweit eindeutig
  force_destroy = true                   # tofu destroy darf auch einen gefuellten Bucket loeschen
}

resource "aws_s3_bucket_website_configuration" "frontend" {
  bucket = aws_s3_bucket.frontend.id

  index_document {
    suffix = "index.html"
  }
}

resource "aws_s3_bucket_public_access_block" "frontend" {
  bucket                  = aws_s3_bucket.frontend.id
  block_public_acls       = false
  block_public_policy     = false
  ignore_public_acls      = false
  restrict_public_buckets = false
}

resource "aws_s3_bucket_policy" "frontend" {
  bucket = aws_s3_bucket.frontend.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Sid       = "PublicRead"
      Effect    = "Allow"
      Principal = "*"
      Action    = "s3:GetObject"
      Resource  = "${aws_s3_bucket.frontend.arn}/*"
    }]
  })

  depends_on = [aws_s3_bucket_public_access_block.frontend]
}

# Nur die Infrastruktur kennt die Adresse der Instanz. Das Deployment ueberspringt diese
# Datei (aws s3 sync --exclude config.js), das Release-Archiv enthaelt sie nicht.
resource "aws_s3_object" "config" {
  bucket       = aws_s3_bucket.frontend.id
  key          = "config.js"
  content_type = "application/javascript"
  content      = <<-EOT
    window.TICTACTEST_CONFIG = {
      consoleUrl: "http://${aws_eip.console.public_ip}",
      versionUrl: "http://${aws_eip.console.public_ip}/version.txt",
    };
  EOT
}
