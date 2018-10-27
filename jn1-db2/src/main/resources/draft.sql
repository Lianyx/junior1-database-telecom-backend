CREATE TABLE user_calls (
  username   VARCHAR(20),
  begin_time DATETIME,
  end_time   DATETIME
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

CREATE TABLE user_local_traffic (
  username     VARCHAR(20),
  request_time DATETIME,
  mb           DOUBLE
)
  ENGINE = InnoDB
  CHARACTER SET = utf8;
;

CREATE TABLE user_domestic_traffic (
  username     VARCHAR(20),
  request_time DATETIME,
  mb           DOUBLE
)
  ENGINE = InnoDB
  CHARACTER SET = utf8;
;

CREATE TABLE basic (
  begin_time                   DATETIME,
  call_cost_per_min            INT,
  text_cost_per_message        INT,
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
  free_messages         DOUBLE,
  free_local_traffic    DOUBLE,
  free_domestic_traffic DOUBLE
)
  ENGINE = InnoDB
  CHARACTER SET = utf8;
;

CREATE TABLE operation (
  id               INT AUTO_INCREMENT PRIMARY KEY,
  username         VARCHAR(20),
  operation        ENUM ('order', 'cancel'),
  combo_id         INT,
  operation_time  DATETIME,
  effectuate_month DATETIME
)
  ENGINE = InnoDB
  CHARACTER SET = utf8;

INSERT INTO operation (username, operation, combo_id, operation_time, effectuate_month) VALUES (?, ?, ?, ?, ?);
INSERT INTO user_calls VALUES (?, ?, ?);
INSERT INTO user_texts VALUES (?, ?);
INSERT INTO user_local_traffic VALUES (?, ?, ?);
INSERT INTO user_domestic_traffic VALUES (?, ?, ?);
INSERT INTO basic VALUES (?, ?, ?, ?, ?);
INSERT INTO combo (cost_per_month, free_call_min, free_messages, free_local_traffic, free_domestic_traffic)
VALUES (?, ?, ?, ?, ?);

SELECT * FROM operation WHERE username = ?;
SELECT * FROM user_calls WHERE username = ? AND (? < begin_time < ? OR ? < end_time < ?);
-- 這個获得之後，头尾还都要处理一下
-- 只有兩個有一個在当前月的范围之间就可以，<=什么的就不管了。
-- 应该也可以用 YEAR 和 MONTH 兩個來判断
SELECT * FROM user_texts WHERE username = ? AND ? < send_time < ?;
SELECT * FROM user_local_traffic WHERE username = ? AND ? < request_time < ?;
SELECT * FROM user_domestic_traffic WHERE username = ? AND ? < request_time < ?;


SELECT * FROM basic WHERE begin_time = ALL (SELECT MAX(begin_time) FROM basic WHERE begin_time <= ?);

SELECT * FROM combo
WHERE id IN (SELECT combo_id FROM operation o1
             WHERE effectuate_month <= ? AND operation = 'order'
                   AND NOT EXISTS(SELECT * FROM operation o2
                                  WHERE effectuate_month <= ? AND o1.combo_id = o2.combo_id
                                        AND o1.operation_time < o2.operation_time)
);
-- 兩個?都是要查询的那個月的1号0点
-- sum的事让java做吧











