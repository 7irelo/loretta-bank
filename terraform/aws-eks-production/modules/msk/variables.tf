variable "project" {
  description = "Project name for resource naming."
  type        = string
}

variable "environment" {
  description = "Environment name."
  type        = string
}

variable "vpc_id" {
  description = "VPC ID for the MSK cluster."
  type        = string
}

variable "private_subnet_ids" {
  description = "Private subnet IDs for the MSK brokers (exactly 3 for 3-broker setup)."
  type        = list(string)
}

variable "allowed_security_group_ids" {
  description = "Security group IDs allowed to connect to Kafka brokers."
  type        = list(string)
}

variable "kafka_version" {
  description = "Apache Kafka version."
  type        = string
  default     = "3.6.0"
}

variable "instance_type" {
  description = "MSK broker instance type."
  type        = string
  default     = "kafka.t3.small"
}

variable "number_of_broker_nodes" {
  description = "Number of Kafka broker nodes."
  type        = number
  default     = 3
}

variable "ebs_volume_size" {
  description = "EBS volume size in GB per broker."
  type        = number
  default     = 100
}

variable "tags" {
  description = "Common tags to apply to all resources."
  type        = map(string)
  default     = {}
}
