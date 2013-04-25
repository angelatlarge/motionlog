# Source EPS images, collected via wildcard
SRC_EPS=$(wildcard *.eps)
#~ SRC_EPS=test.eps
BBX_EPSDIR=./bbxeps

# The drawable directories
DRAWABLE_ROOTDIR=../res
LDPI_DIR=$(DRAWABLE_ROOTDIR)/drawable-ldpi
MDPI_DIR=$(DRAWABLE_ROOTDIR)/drawable-mdpi
HDPI_DIR=$(DRAWABLE_ROOTDIR)/drawable-hdpi
XDPI_DIR=$(DRAWABLE_ROOTDIR)/drawable-xhdpi

# Each image output
LDPI_PNGS=$(patsubst %.eps,$(LDPI_DIR)/%.png,$(notdir $(SRC_EPS)))
MDPI_PNGS=$(patsubst %.eps,$(MDPI_DIR)/%.png,$(notdir $(SRC_EPS)))
HDPI_PNGS=$(patsubst %.eps,$(HDPI_DIR)/%.png,$(notdir $(SRC_EPS)))
XDPI_PNGS=$(patsubst %.eps,$(XDPI_DIR)/%.png,$(notdir $(SRC_EPS)))
ALL_PNGS=$(LDPI_PNGS) $(MDPI_PNGS) $(HDPI_PNGS) $(XDPI_PNGS) 
#~ ALL_PNGS=$(LDPI_PNGS)

#~ $(warning $(ALL_PNGS))

#~ all:	AndroidRasterImages

.PRECIOUS: $(BBX_EPSDIR)/%.eps

AndroidRasterImages: $(ALL_PNGS)

$(BBX_EPSDIR): 
	mkdir -p $(BBX_EPSDIR)

$(BBX_EPSDIR)/%.eps : %.eps $(BBX_EPSDIR)
	./addbb.sh $< $@

$(LDPI_DIR)/%.png : $(BBX_EPSDIR)/%.eps
	convert 								\
		-define png:alpha 					\
		-colorspace RGB 					\
		-density 120 						\
		$< 									\
		$@

$(MDPI_DIR)/%.png : $(BBX_EPSDIR)/%.eps
	convert 								\
		-define png:alpha 					\
		-colorspace RGB 					\
		-density 160 						\
		$< 									\
		$@

$(HDPI_DIR)/%.png : $(BBX_EPSDIR)/%.eps
	convert 								\
		-define png:alpha 					\
		-colorspace RGB 					\
		-density 240 						\
		$< 									\
		$@

$(XDPI_DIR)/%.png : $(BBX_EPSDIR)/%.eps
	convert 								\
		-define png:alpha 					\
		-colorspace RGB 					\
		-density 320 						\
		$< 									\
		$@


#~ ldpi (120 dpi)
#~ (Low density screen) 	
#~ mdpi (160 dpi)
#~ (Medium density screen) 	
#~ hdpi (240 dpi)
#~ (High density screen) 	
#~ xhdpi (320 dpi)
#~ (Extra-high density screen)
