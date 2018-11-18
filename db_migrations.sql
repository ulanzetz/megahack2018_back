create table accounts
(
  login    varchar not null
    constraint accounts_pkey
    primary key,
  password varchar not null,
  name     varchar not null,
  surname  varchar not null,
  email    varchar not null
);

create unique index accounts_email_uindex
  on accounts (email);

create table mentors
(
  login       varchar not null
    constraint mentors_pk
    primary key
    constraint mentors_accounts_login_fk
    references accounts,
  tag         varchar not null,
  description varchar not null
);

create table developers
(
  login  varchar not null
    constraint workers_pkey
    primary key
    constraint workers_accounts_login_fk
    references accounts,
  tag    varchar not null,
  mentor varchar
    constraint developers_mentors_login_fk
    references mentors
);

create table reporters
(
  login varchar not null
    constraint reporters_pk
    primary key
    constraint reporters_accounts_login_fk
    references accounts
);


create table tasks
(
  id          serial                              not null
    constraint tasks_pkey
    primary key,
  label       varchar                             not null,
  developer   varchar
    constraint tasks_developers_login_fk
    references developers,
  reviewer    varchar
    constraint tasks_mentors_login_fk
    references mentors,
  description varchar,
  reporter    varchar                             not null
    constraint tasks_reporters_login_fk
    references reporters,
  x           integer default 0                   not null,
  image_id    integer,
  tag         varchar                             not null,
  task_type   varchar                             not null,
  price       integer                             not null,
  created     timestamp default CURRENT_TIMESTAMP not null,
  deadline    timestamp                           not null
);


create unique index tasks_id_uindex
  on tasks (id);

create table chats
(
  id     serial  not null
    constraint chats_pkey
    primary key,
  first  varchar not null
    constraint chats_accounts_login_fk
    references accounts,
  second varchar not null
    constraint chats_accounts_login_fk_2
    references accounts,
  constraint chats_pk
  unique (first, second)
);

create unique index chats_id_uindex
  on chats (id);

create table messages
(
  sender       varchar                             not null
    constraint messages_accounts_login_fk
    references accounts,
  ts           timestamp default CURRENT_TIMESTAMP not null,
  text         varchar,
  chat_id      integer                             not null
    constraint messages_chats_id_fk
    references chats,
  subject_read boolean default false               not null,
  constraint messages_pk
  unique (chat_id, text, ts, sender)
);
