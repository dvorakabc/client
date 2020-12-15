
source "scripts/utils.sh" # include check_var

IFS='.'
read -ra ADDR <<<"$1"

echo "${ADDR[1]}.$((${ADDR[2]} + 1))" > "version.txt"
