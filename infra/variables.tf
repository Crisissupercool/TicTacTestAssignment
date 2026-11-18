variable "region" {
  description = "AWS-Region (Learner Lab: us-east-1 oder us-west-2)"
  type        = string
  default     = "us-east-1"
}

variable "instance_type" {
  description = "Instanzgroesse. Jede Partie im Browser ist eine eigene JVM."
  type        = string
  default     = "t3.micro"
}

variable "github_repo" {
  description = "owner/name des Repositories, aus dessen Releases die Instanz das Jar laedt (muss oeffentlich sein)"
  type        = string
  default     = "Crisissupercool/TicTacTestAssignment"
}

variable "ttyd_version" {
  description = "Feste Version der Web-Konsole ttyd"
  type        = string
  default     = "1.7.7"
}

variable "ttyd_sha256" {
  description = "SHA-256 des Binaries ttyd.x86_64 dieser Version (aus der Datei SHA256SUMS des ttyd-Releases)"
  type        = string
  default     = "8a217c968aba172e0dbf3f34447218dc015bc4d5e59bf51db2f2cd12b7be4f55"
}
