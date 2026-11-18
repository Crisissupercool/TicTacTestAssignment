output "play_url" {
  description = "Hier spielt man (Frontend)"
  value       = aws_cloudformation_stack.frontend.outputs["WebsiteURL"]
}

output "console_url" {
  description = "Web-Konsole"
  value       = "http://${aws_eip.console.public_ip}"
}

output "version_url" {
  description = "Abfrage der laufenden Backend-Version"
  value       = "http://${aws_eip.console.public_ip}/version.txt"
}

output "bucket_name" {
  description = "Bucket des Frontends (fuer das Deployment)"
  value       = aws_cloudformation_stack.frontend.outputs["BucketName"]
}

output "instance_id" {
  description = "ID der Instanz (fuer SSM im Deployment)"
  value       = aws_instance.console.id
}
