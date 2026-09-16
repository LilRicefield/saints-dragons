package com.leon.saintsdragons.server.entity.part;

import java.util.*;

public final class IgnivorusHitboxes {
    public record Region(String name, float damage, String... bones) {}
    public static final List<Region> REGIONS = List.of(
            new Region("head", 1.25F, "headController", "lowerJawController"),
            new Region("neck1", 1.1F, "neck1Controller"),
            new Region("neck2", 1.1F, "neck2Controller"),
            new Region("neck3", 1.1F, "neck3Controller"),
            new Region("neck4", 1.1F, "neck4Controller"),
            new Region("chest", 1.0F, "heightController"),
            new Region("body", 1.0F, "middlebody"),
            new Region("hip", 1.0F, "hip"),
            new Region("leftWing", 0.9F, "leftwing"),
            new Region("leftWingOuter", 0.9F, "leftwingjoint", "leftfinger"),
            new Region("leftWingTip", 0.9F, "leftinnerphalanges", "leftphalanges", "leftmiddlephalanges", "leftouterphalanges", "leftouterphanlangesBone"),
            new Region("rightWing", 0.9F, "rightwing"),
            new Region("rightWingOuter", 0.9F, "rightwingjoint", "rightfinger"),
            new Region("rightWingTip", 0.9F, "rightinnerphalanges", "rightphalanges", "rightmiddlephalanges", "rightouterphalanges", "rightouterphalangesBone"),
            new Region("tail1", 0.85F, "tail1"),
            new Region("tail2", 0.8F, "tail2"),
            new Region("tail3", 0.8F, "tail3"),
            new Region("tail4", 0.75F, "tail4"),
            new Region("leftFrontLeg", 1.0F, "leftfrontleg", "leftfrontknee2"),
            new Region("leftFrontFoot", 1.0F, "leftfrontankle2", "leftfrontfeet"),
            new Region("rightFrontLeg", 1.0F, "rightfrontleg", "rightfrontknee2"),
            new Region("rightFrontFoot", 1.0F, "rightfrontankle2", "rightfontfeet"),
            new Region("leftBackLeg", 1.0F, "leftbackleg", "leftbackknee"),
            new Region("leftBackFoot", 1.0F, "leftbackankle", "leftbackfeet"),
            new Region("rightBackLeg", 1.0F, "rightbackleg", "rightbackknee"),
            new Region("rightBackFoot", 1.0F, "rightbackankle", "rightbackfeet")
    );
    public static final Set<String> BONES;
    static {
        Set<String> names = new HashSet<>(Set.of("fireBone", "headController"));
        for (Region region : REGIONS) Collections.addAll(names, region.bones);
        BONES = Set.copyOf(names);
    }
    private static final class RigHolder {
        private static final CollisionRig RIG = CollisionRig.load("ignivorus", BONES);
    }
    public static CollisionRig rig() { return RigHolder.RIG; }
    private IgnivorusHitboxes() {}
}
