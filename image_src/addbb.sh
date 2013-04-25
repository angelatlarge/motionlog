TEMPFILENAME=temporary.eps

if [ $# -lt 1 ] ; then
	echo "No arguments provided"
	exit 1 ;
elif [ $# -lt 2 ] ; then
	cp $1 $TEMPFILENAME
	INFILENAME=$TEMPFILENAME
	OUTFILENAME=$1
else
	INFILENAME=$1
	OUTFILENAME=$2
fi
# Low res bounding box
LORESBOUNDINGBOX=`grep 'AI._Cropmarks' $INFILENAME | awk '{for(i=2;i<=NF;i++){printf "%d ", $i}; printf "\n"}'`
# For high-res bounding box
HIRESBOUNDINGBOX=`grep 'AI._Cropmarks' $INFILENAME | awk '{out=$2; for(i=3;i<=NF;i++){out=out" "$i}; print out}'`
#~ cat $INFILENAME.eps | sed '/%%BoundingBox/c '"%%BoundingBox: $LORESBOUNDINGBOX\n%%HiResBoundingBox: $HIRESBOUNDINGBOX" > out.eps
cat $INFILENAME \
	| sed \
		-e '/%%HiResBoundingBox/d' \
		-e '/%%BoundingBox/c '"%%BoundingBox: $LORESBOUNDINGBOX\n%%HiResBoundingBox: $HIRESBOUNDINGBOX" \
	> $OUTFILENAME

if test $# -lt 2 ; then
rm -f $TEMPFILENAME
fi