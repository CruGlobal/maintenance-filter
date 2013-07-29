create table if not exists DatabaseVersion (
 id varchar(50) primary key,
 version bigint not null
 );

create table if not exists MaintenanceWindow ( 
  id varchar(50) primary key,  
  shortMessage varchar(200) not null,  
  longMessage varchar(2000) not null,  
  beginAt timestamp,  
  endAt timestamp,
  filterName varchar(200)
  );

