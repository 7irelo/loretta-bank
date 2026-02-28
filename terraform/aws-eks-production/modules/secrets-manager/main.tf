################################################################################
# Secrets
################################################################################

resource "aws_secretsmanager_secret" "jwt" {
  name                    = "${var.project}/${var.environment}/jwt-secret"
  description             = "JWT signing secret for ${var.project} ${var.environment}"
  recovery_window_in_days = 7

  tags = var.tags
}

resource "aws_secretsmanager_secret_version" "jwt" {
  secret_id = aws_secretsmanager_secret.jwt.id
  secret_string = jsonencode({
    jwt-secret = "REPLACE_ME_WITH_REAL_SECRET"
  })

  lifecycle {
    ignore_changes = [secret_string]
  }
}

resource "aws_secretsmanager_secret" "bootstrap_credentials" {
  name                    = "${var.project}/${var.environment}/bootstrap-credentials"
  description             = "Bootstrap credentials for ${var.project} ${var.environment} services"
  recovery_window_in_days = 7

  tags = var.tags
}

resource "aws_secretsmanager_secret_version" "bootstrap_credentials" {
  secret_id = aws_secretsmanager_secret.bootstrap_credentials.id
  secret_string = jsonencode({
    redis-password        = "REPLACE_ME"
    kafka-bootstrap-url   = "REPLACE_ME"
    eureka-default-zone   = "REPLACE_ME"
    spring-mail-username  = "REPLACE_ME"
    spring-mail-password  = "REPLACE_ME"
  })

  lifecycle {
    ignore_changes = [secret_string]
  }
}

################################################################################
# IRSA Role for External Secrets Operator
################################################################################

data "aws_iam_policy_document" "eso_assume_role" {
  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]
    effect  = "Allow"

    principals {
      type        = "Federated"
      identifiers = [var.oidc_provider_arn]
    }

    condition {
      test     = "StringEquals"
      variable = "${var.oidc_provider_url}:sub"
      values   = ["system:serviceaccount:${var.kubernetes_namespace}:${var.kubernetes_service_account}"]
    }

    condition {
      test     = "StringEquals"
      variable = "${var.oidc_provider_url}:aud"
      values   = ["sts.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "eso" {
  name               = "${var.project}-${var.environment}-eso-irsa"
  assume_role_policy = data.aws_iam_policy_document.eso_assume_role.json

  tags = var.tags
}

data "aws_iam_policy_document" "eso_policy" {
  statement {
    sid    = "AllowSecretsManagerRead"
    effect = "Allow"
    actions = [
      "secretsmanager:GetSecretValue",
      "secretsmanager:DescribeSecret",
      "secretsmanager:ListSecretVersionIds",
    ]
    resources = [
      "arn:aws:secretsmanager:*:*:secret:${var.project}/${var.environment}/*",
    ]
  }

  statement {
    sid    = "AllowListSecrets"
    effect = "Allow"
    actions = [
      "secretsmanager:ListSecrets",
    ]
    resources = ["*"]
  }
}

resource "aws_iam_policy" "eso" {
  name   = "${var.project}-${var.environment}-eso-secrets-policy"
  policy = data.aws_iam_policy_document.eso_policy.json

  tags = var.tags
}

resource "aws_iam_role_policy_attachment" "eso" {
  role       = aws_iam_role.eso.name
  policy_arn = aws_iam_policy.eso.arn
}
