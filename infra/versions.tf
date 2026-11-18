terraform {
  required_version = ">= 1.6"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

# Die Zugangsdaten kommen aus den Umgebungsvariablen AWS_ACCESS_KEY_ID,
# AWS_SECRET_ACCESS_KEY und AWS_SESSION_TOKEN (Learner Lab), nie aus dem Code.
provider "aws" {
  region = var.region
}
