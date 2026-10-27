"""Runtime configuration. Secrets come only from the environment and are never logged."""

from functools import lru_cache

from pydantic import Field, SecretStr
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=None, extra="ignore")

    # Identity: tokens minted by api-gateway (and our own downstream tokens) use this key.
    internal_jwt_secret: SecretStr = Field(alias="INTERNAL_JWT_SECRET")

    anthropic_api_key: SecretStr | None = Field(default=None, alias="ANTHROPIC_API_KEY")
    openai_api_key: SecretStr | None = Field(default=None, alias="OPENAI_API_KEY")
    deepgram_api_key: SecretStr | None = Field(default=None, alias="DEEPGRAM_API_KEY")

    # Optional fallback: the same Claude models through Amazon Bedrock.
    bedrock_region: str | None = Field(default=None, alias="BEDROCK_REGION")

    customer_service_url: str = Field(default="http://localhost:8082", alias="CUSTOMER_SERVICE_URL")
    account_service_url: str = Field(default="http://localhost:8083", alias="ACCOUNT_SERVICE_URL")
    transaction_service_url: str = Field(
        default="http://localhost:8084", alias="TRANSACTION_SERVICE_URL"
    )
    redis_url: str | None = Field(default=None, alias="REDIS_URL")

    # Per-user daily budget for assistant usage, in US cents.
    daily_user_budget_cents: int = Field(default=50, alias="AI_DAILY_USER_BUDGET_CENTS")
    request_timeout_seconds: float = Field(default=30.0, alias="AI_REQUEST_TIMEOUT_SECONDS")
    otlp_endpoint: str | None = Field(default=None, alias="OTEL_EXPORTER_OTLP_ENDPOINT")

    @property
    def llm_enabled(self) -> bool:
        return self.anthropic_api_key is not None and bool(
            self.anthropic_api_key.get_secret_value()
        )


@lru_cache
def get_settings() -> Settings:
    return Settings()  # type: ignore[call-arg]
