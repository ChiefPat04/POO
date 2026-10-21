package com.jungleattack.server.model;

/**
 * Las 7 especies que se pueden escoger. Todas son animales nativos de Costa Rica.
 * Cada una tiene un nombre para mostrar y el nombre de su imagen.
 */
public enum Species {
    JAGUAR("Jaguar", "jaguar.svg"),
    SLOTH("Perezoso", "sloth.svg"),
    HOWLER_MONKEY("Mono congo", "howler_monkey.svg"),
    TAPIR("Danta", "tapir.svg"),
    TOUCAN("Tucán", "toucan.svg"),
    RED_EYED_FROG("Rana calzonuda", "red_eyed_frog.svg"),
    COATI("Pizote", "coati.svg");

    private final String displayName;
    private final String imageFile;

    Species(String displayName, String imageFile) {
        this.displayName = displayName;
        this.imageFile = imageFile;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getImageFile() {
        return imageFile;
    }
}
