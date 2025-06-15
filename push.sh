#!/bin/bash

if [ $# -lt 1 ] 
then 
echo "Passez le verbe de l'action..."
echo "push / pull"
exit 0
fi

VERBE="$1"
URL="https://github.com/isi-hop/isiETL.git"

if [ "$VERBE" = "pull" ]; then
    echo "pull action!"
elif [ "$VERBE" = "push" ]; then
    echo "push action!"
else
    echo "Le verbe est inconnu, utilisez pull ou push!"
    exit 1
fi

git $VERBE $URL
