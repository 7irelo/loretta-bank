output "db_instance_endpoint" {
  description = "The connection endpoint for the RDS instance."
  value       = aws_db_instance.this.endpoint
}

output "db_instance_address" {
  description = "The hostname of the RDS instance."
  value       = aws_db_instance.this.address
}

output "db_instance_port" {
  description = "The port of the RDS instance."
  value       = aws_db_instance.this.port
}

output "db_instance_name" {
  description = "The name of the default database."
  value       = aws_db_instance.this.db_name
}

output "db_instance_identifier" {
  description = "The RDS instance identifier."
  value       = aws_db_instance.this.identifier
}

output "db_master_user_secret_arn" {
  description = "ARN of the Secrets Manager secret containing the master password."
  value       = aws_db_instance.this.master_user_secret[0].secret_arn
}

output "db_security_group_id" {
  description = "Security group ID for the RDS instance."
  value       = aws_security_group.rds.id
}
