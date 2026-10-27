-- Event-carried state: notification-service keeps its own copy of who each customer is, so
-- notifications reach the right user without synchronous calls to customer-service.
CREATE TABLE customer_directory (
    customer_id BIGINT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    email       VARCHAR(255) NOT NULL,
    first_name  VARCHAR(100)
);

ALTER TABLE notifications
    ADD COLUMN read_at TIMESTAMP;

-- At-least-once delivery from Kafka must not produce duplicate notifications.
CREATE UNIQUE INDEX uq_notifications_event_user ON notifications(event_id, user_id);
