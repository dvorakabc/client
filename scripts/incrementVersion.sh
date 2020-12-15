
source "scripts/utils.sh" # include check_var

IFS='.'
read -ra ADDR <<<"$1"
IFS='-'
read -ra ADDR2 <<<${ADDR[2]}

echo "${ADDR[1]}.$((${ADDR2[0]} + 1))" > "version.txt"
