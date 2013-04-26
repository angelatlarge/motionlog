TEMPFILENAME=temporary_addbb.eps

if [ $# -lt 1 ] ; then
#	Using standard input and standard output
	cat > $TEMPFILENAME
	INFILENAME=$TEMPFILENAME
	OUTFILENAME=/dev/stdout
elif [ $# -lt 2 ] ; then
#	One argument privided, use it as input, print to stdout
	INFILENAME=$1
	OUTFILENAME='/dev/stdout'
elif [ "$1" = "$2" ]; then
#	Two arguments that are the same
	cp $1 $TEMPFILENAME
	INFILENAME=$TEMPFILENAME
	OUTFILENAME=$2
else
#	Two different arguments
	INFILENAME=$1
	OUTFILENAME=$2
fi
# Low res bounding box
LORESBOUNDINGBOX=`cat $INFILENAME | grep 'AI._Cropmarks' | awk '{for(i=2;i<=NF;i++){printf "%d ", $i}; printf "\n"}'`
# For high-res bounding box
HIRESBOUNDINGBOX=`cat $INFILENAME | grep 'AI._Cropmarks'  | awk '{out=$2; for(i=3;i<=NF;i++){out=out" "$i}; print out}'`
#~ cat $INFILENAME.eps | sed '/%%BoundingBox/c '"%%BoundingBox: $LORESBOUNDINGBOX\n%%HiResBoundingBox: $HIRESBOUNDINGBOX" > out.eps
#~ echo cat $INFILENAME
#~ cat $INFILENAME
cat $INFILENAME \
	| sed \
		-e '/%%HiResBoundingBox/d' \
		-e '/%%BoundingBox/c '"%%BoundingBox: $LORESBOUNDINGBOX\n%%HiResBoundingBox: $HIRESBOUNDINGBOX" \
	> $OUTFILENAME

# [ $# -gt 1 ] && 
if [ "$1" = "$2" ]; then
rm -f $TEMPFILENAME
fi