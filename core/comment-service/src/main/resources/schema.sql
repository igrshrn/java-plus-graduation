CREATE TABLE IF NOT EXISTS comments
(
    id
    BIGINT
    GENERATED
    BY
    DEFAULT AS
    IDENTITY
    PRIMARY
    KEY,
    text
    VARCHAR
(
    7000
) NOT NULL,
    author_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    created TIMESTAMP NOT NULL,
    updated TIMESTAMP,
    status VARCHAR
(
    20
) NOT NULL DEFAULT 'PENDING'
    );