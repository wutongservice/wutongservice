
-- ts2str
DROP FUNCTION IF EXISTS ts2str;
CREATE FUNCTION ts2str(ts BIGINT) RETURNS VARCHAR(255) DETERMINISTIC RETURN FROM_UNIXTIME(ts/1000, '%Y-%m-%d %H:%m:%s');

-- find_account_by_ticket
DROP PROCEDURE IF EXISTS find_account_by_ticket;
CREATE PROCEDURE find_account_by_ticket(ticket VARCHAR(128))
  SELECT A.id,A.name,A.email,ts2str(A.created_at) AS created_at FROM accounts A, signin_tickets T WHERE A.id=T.account_id AND T.ticket=ticket;