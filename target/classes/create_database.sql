DROP DATABASE IF EXISTS dzrt_db;
DROP user IF EXISTS 'dzrt'@localhost;
create database IF NOT EXISTS dzrt_db;
create user IF NOT EXISTS dzrt@localhost identified by '123654';
grant all privileges on dzrt_db.* To dzrt@localhost;