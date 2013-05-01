# Source EPS images, collected via wildcard
#~ SRC_EPS=$(wildcard *.eps)
SRC_EPS=					\
	ic_dialog_settings.eps		\
	ic_dialog_speed_fast.eps	\
	ic_dialog_speed_medium.eps	\
	ic_dialog_speed_slow.eps	\
	ic_dialog_speed_vfast.eps	\
	ic_dialog_target_both.eps	\
	ic_dialog_target_file.eps	\
	ic_dialog_target_graph.eps	\
	ic_stat_notify_logging.eps

#~ SRC_EPS=test.eps
BBX_EPSDIR=./bbxeps

# The drawable directories
DRAWABLE_ROOTDIR=../res
LDPI_DIR=$(DRAWABLE_ROOTDIR)/drawable-ldpi
MDPI_DIR=$(DRAWABLE_ROOTDIR)/drawable-mdpi
HDPI_DIR=$(DRAWABLE_ROOTDIR)/drawable-hdpi
XDPI_DIR=$(DRAWABLE_ROOTDIR)/drawable-xhdpi

# Each image output
# The _DPI_PNGs says: we need a file in the _DPI_DIR with this name
# The recipe below says how to make the PNG from EPS
LDPI_PNGS=$(patsubst %.eps,$(LDPI_DIR)/%.png,$(notdir $(SRC_EPS)))
MDPI_PNGS=$(patsubst %.eps,$(MDPI_DIR)/%.png,$(notdir $(SRC_EPS)))
HDPI_PNGS=$(patsubst %.eps,$(HDPI_DIR)/%.png,$(notdir $(SRC_EPS)))
XDPI_PNGS=$(patsubst %.eps,$(XDPI_DIR)/%.png,$(notdir $(SRC_EPS)))
ALL_PNGS=$(LDPI_PNGS) $(MDPI_PNGS) $(HDPI_PNGS) $(XDPI_PNGS) 



DELETE=rm -f

.PRECIOUS: $(BBX_EPSDIR)/%.eps

all:	AndroidRasterImages
AndroidRasterImages: $(ALL_PNGS)

$(BBX_EPSDIR): 
	mkdir -p $(BBX_EPSDIR)

CONVERT_OPTS=								\
		-define png:alpha 					\
		-colorspace RGB

$(LDPI_DIR)/%.png : $(BBX_EPSDIR)/%.eps
	convert $(CONVERT_OPTS) -density 120 $< $@

$(MDPI_DIR)/%.png : $(BBX_EPSDIR)/%.eps
	convert $(CONVERT_OPTS) -density 160 $< $@

$(HDPI_DIR)/%.png : $(BBX_EPSDIR)/%.eps
	convert $(CONVERT_OPTS) -density 240 $< $@

$(XDPI_DIR)/%.png : $(BBX_EPSDIR)/%.eps
	convert $(CONVERT_OPTS) -density 320 $< $@

#~ ldpi (120 dpi)
#~ (Low density screen) 	
#~ mdpi (160 dpi)
#~ (Medium density screen) 	
#~ hdpi (240 dpi)
#~ (High density screen) 	
#~ xhdpi (320 dpi)
#~ (Extra-high density screen)

clean:
	$(DELETE) $(ALL_PNGS)
	$(DELETE) -fR $(BBX_EPSDIR)/*
	