create role read_only_group;
grant usage on schema public to read_only_group;
grant select on all tables in schema public to read_only_group;
grant select on all sequences in schema public to read_only_group;
alter default privileges in schema public grant select on tables to read_only_group;
alter default privileges in schema public grant select on sequences to read_only_group;

create role read_write_group in group read_only_group;
grant insert, update, delete, truncate on all tables in schema public to read_write_group;
alter default privileges in schema public grant insert, update, delete, truncate on tables to read_write_group;

create or replace function create_user(username text, password text, read_only boolean) returns void as
$$
declare
    role_name text;
    role_group text;
begin
    if (read_only) then
        role_name = username || '_ro';
        role_group = 'read_only_group';
    else
        role_name = username || '_rw';
        role_group = 'read_write_group';
    end if;
    if exists (select 1
               from pg_catalog.pg_roles
               where rolname = role_name) then

        raise notice 'role % already exists.', role_name;
    else
        begin
            execute 'create role ' || role_name || ' with login password ''' || password || ''' in group ' || role_group;
        exception
            when duplicate_object then
                raise notice 'role % already exists.', role_name;
        end;
    end if;
end ;
$$ language plpgsql;


-- example usage to create user
/*
   select create_user('first_last', 'secret_password', true);
   will create first_last_ro (read only)

   select create_user('first_last', 'secret_password', false);
   will create first_last_rw (read write)
*/

-- to remove a user
/*
    drop user 'username_ro'; -- must be the full username
*/

-- to view users with login access
/*
    select * from pg_roles where rolcanlogin = true;
*/