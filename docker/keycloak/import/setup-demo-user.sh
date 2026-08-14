#!/bin/bash
# =============================================================================
# Enterprise Order Processing Platform - Keycloak Demo User Setup
# =============================================================================
#
# Creates a demo user in the enterprise-platform realm so the platform works
# out of the box after `docker-compose up`.
#
# The password is read from the KEYCLOAK_DEMO_PASSWORD environment variable
# (see .env). It is never committed to the repository.
#
# Usage (inside the running keycloak container):
#   docker-compose exec keycloak /bin/bash /opt/keycloak/data/import/setup-demo-user.sh
# =============================================================================

set -e

REALM="enterprise-platform"
USERNAME="demouser"
EMAIL="demouser@enterprise.local"
FIRST_NAME="Demo"
LAST_NAME="User"
ROLES=("CUSTOMER" "OPERATOR")

KCADM="/opt/keycloak/bin/kcadm.sh"

: "${KEYCLOAK_ADMIN:?KEYCLOAK_ADMIN must be set in .env}"
: "${KEYCLOAK_ADMIN_PASSWORD:?KEYCLOAK_ADMIN_PASSWORD must be set in .env}"
: "${KEYCLOAK_DEMO_PASSWORD:?KEYCLOAK_DEMO_PASSWORD must be set in .env}"

# Authenticate as the admin user (from .env)
"$KCADM" config credentials \
  --server http://localhost:8080 \
  --realm master \
  --user "$KEYCLOAK_ADMIN" \
  --password "$KEYCLOAK_ADMIN_PASSWORD"

# Create the demo user if it does not exist yet
USER_ID=$("$KCADM" get users -r "$REALM" -q username="$USERNAME" --fields id 2>/dev/null \
  | grep -o '"id"[[:space:]]*:[[:space:]]*"[^"]*"' \
  | head -1 \
  | sed 's/.*"id"[[:space:]]*:[[:space:]]*"//; s/"$//')

if [ -z "$USER_ID" ]; then
  "$KCADM" create users -r "$REALM" \
    -s username="$USERNAME" \
    -s email="$EMAIL" \
    -s firstName="$FIRST_NAME" \
    -s lastName="$LAST_NAME" \
    -s enabled=true \
    -s emailVerified=true >/dev/null
  echo "Demo user '$USERNAME' created."
else
  echo "Demo user '$USERNAME' already exists."
fi

# Set the password
"$KCADM" set-password -r "$REALM" \
  --username "$USERNAME" \
  --new-password "$KEYCLOAK_DEMO_PASSWORD"

echo "Password set for '$USERNAME'."

# Assign roles
for role in "${ROLES[@]}"; do
  "$KCADM" add-roles -r "$REALM" \
    --uusername "$USERNAME" \
    --rolename "$role" >/dev/null
  echo "Role '$role' assigned to '$USERNAME'."
done

echo "Demo user setup complete."
