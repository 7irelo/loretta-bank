output "redis_endpoint" {
  description = "The primary endpoint address of the Redis replication group."
  value       = aws_elasticache_replication_group.this.primary_endpoint_address
}

output "redis_reader_endpoint" {
  description = "The reader endpoint address of the Redis replication group."
  value       = aws_elasticache_replication_group.this.reader_endpoint_address
}

output "redis_port" {
  description = "The port number for the Redis replication group."
  value       = aws_elasticache_replication_group.this.port
}

output "redis_security_group_id" {
  description = "Security group ID for the Redis cluster."
  value       = aws_security_group.redis.id
}
