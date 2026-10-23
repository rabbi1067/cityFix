# Deploy CityFix on Render

The application uses Supabase only for Postgres. New complaint, resolution and
profile images are uploaded to Cloudinary. Only their HTTPS URLs are stored in
Postgres; there are no new image blobs or local upload files.

## Required configuration

Create a Cloudinary account/product environment and find its cloud name, API key
and API secret in the Cloudinary console. Keep the secret on the server.

In Render, set these environment variables:

| Variable | Value |
| --- | --- |
| `DB_URL` | Your Supabase JDBC URL, e.g. `jdbc:postgresql://YOUR_POOLER_HOST:5432/postgres?sslmode=require` |
| `DB_USERNAME` | Your Supabase pooler username, e.g. `postgres.YOUR_PROJECT_REF` |
| `DB_PASSWORD` | Your Supabase database password |
| `SUPER_ADMIN_EMAIL` | The email address for your super admin account |
| `SUPER_ADMIN_PASSWORD` | The password for that super admin account |
| `CLOUDINARY_CLOUD_NAME` | Your Cloudinary cloud name |
| `CLOUDINARY_API_KEY` | Your Cloudinary API key |
| `CLOUDINARY_API_SECRET` | Your Cloudinary API secret |
| `MAIL_USERNAME` | Your sending Gmail address |
| `MAIL_PASSWORD` | Your Gmail App Password |

The database URL must use the JDBC prefix. Username and password are separate
variables; do not put them in the URL. No Supabase storage service key is needed.
All three database variables are required. Source code, examples and the Render
Blueprint contain no project-specific database URL, username or password. The
Blueprint prompts for these values instead of assigning defaults.

## Super admin account

Set `SUPER_ADMIN_EMAIL` and `SUPER_ADMIN_PASSWORD` in Render Environment. On
startup, the app creates that admin if necessary, grants `SUPER_ADMIN`, and
enables the account. For an existing admin with that email, it also synchronizes
the password with the environment value. Only a password hash is stored in the
database. No super admin password is embedded in source, the login page or logs.

As long as these variables remain set, they reapply the configured access and
password on startup. Update the environment password when changing this account's
password, or remove both variables after provisioning to manage it only in the
app. Supplying only one variable is a configuration error.

The default theme is Day mode. An explicitly saved Night mode preference remains
available through the day/night toggle.

## Fixing the old tenant/user error

If the Render log mentions a tenant/user that does not match your current
Supabase project's connection details, it is using stale configuration. Copy the
host, port and username together from your project's **Connect > Session pooler**
dialog. These values must belong to the same project as the password.

On the existing Render service, open **Environment** and check both `DB_URL` /
`DB_USERNAME` and `SPRING_DATASOURCE_URL` / `SPRING_DATASOURCE_USERNAME`, including
linked environment groups. Remove stale overrides or replace them with the
matching host and username from your Supabase Connect dialog. Set `DB_PASSWORD` to the matching
project's password. A `SPRING_DATASOURCE_PASSWORD` override must also be removed
or updated if it exists. Then choose **Save, rebuild, and deploy**, or deploy the
latest commit if there are no environment changes.

Pushing this code does not update variables on an existing manually created
Render service. Set `DB_URL`, `DB_USERNAME` and `DB_PASSWORD` in Render Environment
before deploying this version. There are no database defaults in the app.

## Deployment

1. Push this project's contents to GitHub, with `pom.xml`, `Dockerfile` and
   `render.yaml` at the repository root.
2. Open the Render dashboard, choose **New > Blueprint**, and connect the repo.
3. Fill in the prompted environment variables and deploy. The Blueprint creates
   a Free Docker web service; it does not create a database.

Alternatively, use **New > Web Service**, connect the repo, select **Docker**,
and use `./Dockerfile`. Set the variables above before deployment. Build and
start commands are already in the Dockerfile. If these files are in a subfolder
in GitHub, set that folder as the service's Root Directory.

Render supplies `PORT`, and Spring Boot reads it automatically. After the service
is Live, test registration/login, complaint photos, resolution photos and avatars.
Check that Cloudinary has the images and Postgres has only URL strings. Database
schema updates run at app startup.

## Image delivery and existing data

Homepage images use a maximum width of 480px; detail images use 1200px. Cloudinary
chooses the format and quality (`f_auto,q_auto,c_limit`) and serves them from its
CDN. The original URL remains in Postgres. Existing Supabase image URLs continue
to display; old images are not automatically migrated or deleted.
Existing `/uploads/...` avatar paths still depend on their original local files;
upload those avatars again to move them to Cloudinary before relying on Render.

Replacement images are uploaded before old images are removed. Old Cloudinary
images are removed only after a successful database commit. Failed database
transactions attempt to clean up their new uploads. A cleanup failure is logged
and may require manual removal in Cloudinary.

## Local development

Copy `application-local.properties.example` to `application-local.properties`,
replace the placeholders, and run with Java 25 using `mvnw.cmd spring-boot:run`.
The local configuration is read automatically and ignored by Git and Docker.
Real passwords and API secrets must not be committed.

## Free instance limits

Render Free services sleep after 15 minutes without inbound traffic, so the next
request has a cold start. CDN images reduce download work but do not remove that
startup delay. Gmail SMTP on port 587 is blocked on Free instances; password
reset emails need a paid instance or an HTTPS email API integration.

References: https://render.com/docs/docker, https://render.com/docs/free,
https://cloudinary.com/documentation/image_upload_api_reference.
