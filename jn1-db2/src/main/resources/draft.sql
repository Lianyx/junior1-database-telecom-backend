CREATE TABLE user_calls (
  username   VARCHAR(20),
  begin_time DATETIME,
  duration   DOUBLE
)
  ENGINE = InnoDB
  CHARACTER SET = utf8;
;

CREATE TABLE user_texts (
  username  VARCHAR(20),
  send_time DATETIME
)
  ENGINE = InnoDB
  CHARACTER SET = utf8;
;

CREATE TABLE user_traffic (
  username     VARCHAR(20),
  request_time DATETIME,
  mb           DOUBLE,
  type         ENUM ('domestic', 'local')
)
  ENGINE = InnoDB
  CHARACTER SET = utf8;
;

CREATE TABLE basic (
  begin_time                   DATETIME,
  call_cost_per_min            DOUBLE,
  text_cost_per_message        DOUBLE,
  local_traffic_cost_per_mb    DOUBLE,
  domestic_traffic_cost_per_mb DOUBLE
)
  ENGINE = InnoDB
  CHARACTER SET = utf8;
;

CREATE TABLE combo (
  id                    INT AUTO_INCREMENT PRIMARY KEY,
  cost_per_month        DOUBLE,
  free_call_min         DOUBLE,
  free_messages         INT,
  free_local_traffic    DOUBLE,
  free_domestic_traffic DOUBLE
)
  ENGINE = InnoDB
  CHARACTER SET = utf8;
;

CREATE TABLE operation (
  id              INT AUTO_INCREMENT PRIMARY KEY,
  username        VARCHAR(20),
  operation       ENUM ('order', 'cancel'),
  combo_id        INT,
  operation_time  DATETIME,
  effectuate_time DATETIME
)
  ENGINE = InnoDB
  CHARACTER SET = utf8;
;

INSERT INTO operation (username, operation, combo_id, operation_time, effectuate_time) VALUES (?, ?, ?, ?, ?);
INSERT INTO user_calls VALUES (?, ?, ?);
INSERT INTO user_texts VALUES (?, ?);
INSERT INTO user_traffic VALUES (?, ?, ?, ?);
INSERT INTO basic VALUES (?, ?, ?, ?, ?);
INSERT INTO combo (cost_per_month, free_call_min, free_messages, free_local_traffic, free_domestic_traffic)
VALUES (?, ?, ?, ?, ?);

SELECT * from (SELECT * FROM operation WHERE username = ?) as A JOIN combo ON A.combo_id = combo.id;

SELECT * FROM user_calls WHERE username = ? AND ? <= begin_time AND begin_time < ? ORDER BY begin_time ASC;

SELECT * FROM user_texts WHERE username = ? AND ? <= send_time AND send_time < ? ORDER BY send_time ASC;

SELECT * FROM user_traffic WHERE username = ? AND ? <= request_time AND request_time < ? ORDER BY request_time ASC;


SELECT *
FROM basic
WHERE begin_time = ALL (SELECT MAX(begin_time)
                        FROM basic
                        WHERE begin_time <= ?);

-- 這裡传入的兩個参数（其实是一個），即和effectuate_time比较那個是：下个月的
SELECT
  id,
  effectuate_time,
  cost_per_month,
  free_call_min,
  free_messages,
  free_local_traffic,
  free_domestic_traffic
FROM (SELECT
        combo_id,
        effectuate_time
      FROM operation o1
      WHERE effectuate_time < ? AND operation = 'order'
            AND NOT EXISTS(SELECT *
                           FROM operation o2
                           WHERE effectuate_time < ? AND operation = 'cancel'
                                 AND o1.combo_id = o2.combo_id
                                 AND o1.operation_time <= o2.operation_time)
     ) AS O, combo
WHERE O.combo_id = combo.id
ORDER BY effectuate_time ASC;




INSERT INTO basic VALUES ('1998-01-01 00:00:00', 0.5, 0.1, 2, 5);
INSERT INTO combo VALUES (1, 20, 100, 0, 0, 0);
INSERT INTO combo VALUES (2, 10, 0, 200, 0, 0);
INSERT INTO combo VALUES (3, 10, 0, 0, 1000, 0);
INSERT INTO combo VALUES (4, 50, 0, 0, 500, 2000);
INSERT INTO operation VALUES (1, '189', 'order', 1, '1998-01-01 00:00:00', '1998-01-01 00:00:00');
INSERT INTO operation VALUES (2, '189', 'order', 4, '2018-01-03 00:00:00', '2018-01-03 00:00:00');
INSERT INTO user_traffic VALUES ('189', '2018-01-04 00:00:00', 300, 'local');
INSERT INTO user_traffic VALUES ('189', '2018-01-04 00:00:02', 2000, 'domestic');
INSERT INTO user_traffic VALUES ('189', '2018-01-04 00:00:01', 500, 'local');
INSERT INTO user_calls VALUES ('189', '2018-01-04 00:00:02', 100);
INSERT INTO user_texts VALUES ('189', '2018-01-04 00:00:02');
INSERT INTO user_texts VALUES ('189', '2018-01-04 00:00:05');
INSERT INTO user_texts VALUES ('189', '2018-01-04 00:00:04');


# 查callDetail和trafficDetail

INSERT INTO operation VALUES (3, '189', 'cancel', 4, '2018-01-05 00:00:00', '2018-01-05 00:00:00');

# 再查

INSERT INTO operation VALUES (4, '189', 'order', 4, '2018-01-05 00:00:01', '2018-01-05 00:00:01');











































