#!/bin/bash
gnome-terminal -- bash -c "libretranslate; exec bash"
gnome-terminal -- bash -c "mvn javafx:run; exec bash"

