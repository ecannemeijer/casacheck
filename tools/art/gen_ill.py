#!/usr/bin/env python3
"""Generates the schematic reference illustrations (own artwork, simple vector drawings) as Android vector drawables."""
import os
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../app/src/main/res/drawable")
W, H = 160, 110
WALL = "#E8E1D5"; LINE = "#5B5146"; DARK = "#3A332C"; RED = "#C0392B"; BLUE = "#2E86C1"; GREEN = "#3E7D3A"; BROWN = "#8E5B2E"; SKY = "#D6EAF8"; GREY = "#9E9E9E"

def p(d, fill=None, stroke=None, w=1.5, alpha=None):
    a = f' android:pathData="{d}"'
    if fill: a += f' android:fillColor="{fill}"'
    if alpha: a += f' android:fillAlpha="{alpha}"'
    if stroke: a += f' android:strokeColor="{stroke}" android:strokeWidth="{w}" android:strokeLineCap="round" android:strokeLineJoin="round"'
    return f"    <path{a}/>"

def rect(x, y, rw, rh, **k): return p(f"M{x},{y}h{rw}v{rh}h{-rw}z", **k)
def circle(cx, cy, r, **k): return p(f"M{cx-r},{cy}a{r},{r} 0 1,0 {2*r},0a{r},{r} 0 1,0 {-2*r},0z", **k)

def wall(): return [rect(0, 0, W, H, fill="#FFFFFF"), rect(10, 8, 140, 84, fill=WALL, stroke=LINE), p("M10,92h140", stroke=DARK, w=3)]
def window(x=60, y=24, w=40, h=36): return [rect(x, y, w, h, fill=SKY, stroke=LINE, w=2), p(f"M{x+w/2},{y}v{h}M{x},{y+h/2}h{w}", stroke=LINE, w=1.5)]
def crack(d): return [p(d, stroke=DARK, w=2.2)]

ILL = {
 "rising_damp": wall() + [p("M10,92V60q15,-8 30,0t30,0t30,0t30,0t20,0V92z", fill="#B9A27E", alpha="0.8"), p("M10,60q15,-8 30,0t30,0t30,0t30,0t20,0", stroke=BROWN, w=1.5),
   p("M80,88v-20M74,74l6,-6l6,6", stroke=BLUE, w=2)],
 "penetrating_damp": wall() + window() + [p("M58,62q-6,14 -2,26q16,4 22,-8q4,-10 -6,-18z", fill="#B9A27E", alpha="0.8"), p("M30,20l8,6M28,30l8,6M26,40l8,6", stroke=BLUE, w=2)],
 "salt_deposits": wall() + [p("M10,92V66h140V92z", fill="#D9CBB2"), circle(30, 74, 3, fill="#FFFFFF", stroke=GREY, w=0.8), circle(44, 80, 4, fill="#FFFFFF", stroke=GREY, w=0.8),
   circle(70, 72, 3, fill="#FFFFFF", stroke=GREY, w=0.8), circle(96, 82, 5, fill="#FFFFFF", stroke=GREY, w=0.8), circle(120, 74, 3, fill="#FFFFFF", stroke=GREY, w=0.8), p("M84,70l6,4l-4,6z", fill="#FFFFFF", stroke=GREY, w=0.8)],
 "black_mould": wall() + [p("M10,8h50q-10,14 -30,12q-10,6 -20,0z", fill="#2C2C2C", alpha="0.75"), circle(70, 14, 4, fill="#2C2C2C"), circle(80, 18, 2.5, fill="#2C2C2C"), circle(62, 22, 3, fill="#2C2C2C"),
   circle(20, 30, 3, fill="#2C2C2C"), circle(28, 26, 2, fill="#2C2C2C")],
 "condensation": wall() + window(50, 20, 60, 50) + [p("M56,64q2,-4 4,0q-2,3 -4,0zM70,58q2,-4 4,0q-2,3 -4,0zM88,66q2,-4 4,0q-2,3 -4,0zM100,56q2,-4 4,0q-2,3 -4,0zM62,48q2,-4 4,0q-2,3 -4,0z", fill=BLUE),
   p("M50,72h60v6h-60z", fill="#2C2C2C", alpha="0.6")],
 "roof_leak": [rect(0, 0, W, H, fill="#FFFFFF"), p("M10,40L80,8L150,40z", fill="#B5651D", stroke=LINE), rect(20, 40, 120, 60, fill=WALL, stroke=LINE),
   p("M80,8l-6,32", stroke=BLUE, w=2), p("M74,48q-14,10 -6,20q14,6 18,-6q2,-10 -12,-14z", fill="#B9A27E", alpha="0.85"), p("M76,72q2,-5 4,0q-2,4 -4,0zM76,82q2,-5 4,0q-2,4 -4,0z", fill=BLUE)],
 "roof_tiles": [rect(0, 0, W, H, fill=SKY), p("M0,90L160,30V110H0z", fill="#C0622F")] +
   [p(f"M{x},{90-x*0.375}q6,-6 12,0", stroke="#7A3A16", w=1.4) for x in range(0, 160, 12)] +
   [p(f"M{x},{100-x*0.375}q6,-6 12,0", stroke="#7A3A16", w=1.4) for x in range(6, 160, 12)] + [p("M70,62l12,-4l4,10l-14,4z", fill="#3A332C"), p("M96,48l10,8", stroke=DARK, w=2)],
 "crack_vertical": wall() + crack("M80,10l-2,14l3,10l-3,14l2,12l-3,14l2,16"),
 "crack_horizontal": wall() + crack("M14,58l18,-2l16,3l20,-2l18,2l20,-3l18,2l20,0"),
 "crack_diagonal": wall() + crack("M30,88l12,-12l6,-4l10,-12l8,-6l12,-14l10,-8l12,-14"),
 "crack_ceiling": [rect(0, 0, W, H, fill="#FFFFFF"), p("M0,0h160l-30,40H30z", fill="#F4F1EA", stroke=LINE), p("M30,40H130V110H30z", fill=WALL, stroke=LINE), p("M0,0l30,40M160,0l-30,40", stroke=LINE),
   p("M40,10l18,6l14,-2l16,8l20,-4l16,6", stroke=DARK, w=2.2)],
 "crack_window": wall() + window(56, 28, 48, 40) + crack("M104,28l8,-6l6,-10M56,68l-8,8l-4,12"),
 "crack_facade": [rect(0, 0, W, H, fill="#FFFFFF"), rect(20, 10, 120, 90, fill="#E9C9A0", stroke=LINE)] + window(36, 24, 24, 26) + window(100, 24, 24, 26) +
   [rect(68, 60, 24, 40, fill="#8E5B2E", stroke=LINE)] + crack("M60,50l6,6l2,10M100,50l-4,8l2,8l-6,10l4,8"),
 "crack_foundation": [rect(0, 0, W, H, fill="#FFFFFF"), rect(10, 8, 140, 64, fill=WALL, stroke=LINE), rect(10, 72, 140, 18, fill="#BDBDBD", stroke=LINE), p("M0,90h160v20H0z", fill="#8D6E63"),
   p("M70,72l4,6l-3,6l5,6", stroke=DARK, w=2.4), p("M74,72l-4,-10l6,-8l-4,-10l6,-10", stroke=DARK, w=2.0)],
 "wood_rot": [rect(0, 0, W, H, fill="#FFFFFF"), rect(20, 30, 120, 26, fill="#C49A6C", stroke=LINE), p("M20,38h120M20,48h120", stroke="#A67C52", w=1),
   p("M90,30q10,10 4,26h30q-6,-14 6,-26z", fill="#5D4037"), p("M96,40l6,4M108,36l4,8M116,46l-4,4", stroke="#3E2723", w=1.4)],
 "woodworm": [rect(0, 0, W, H, fill="#FFFFFF"), rect(20, 30, 120, 40, fill="#C49A6C", stroke=LINE), p("M20,42h120M20,56h120", stroke="#A67C52", w=1)] +
   [circle(x, y, 1.8, fill="#3E2723") for x, y in [(40, 38), (52, 46), (66, 36), (74, 52), (88, 44), (100, 60), (112, 38), (122, 50), (58, 62), (84, 64), (130, 62)]] +
   [p("M30,90h40", stroke=GREY), p("M30,96h20", stroke=GREY)],
 "damaged_frame": [rect(0, 0, W, H, fill="#FFFFFF"), rect(46, 10, 68, 90, fill="#A1887F", stroke=LINE, w=2), rect(54, 18, 52, 74, fill=SKY, stroke=LINE),
   p("M46,84l10,6l-4,10h-6z", fill="#5D4037"), p("M60,30l12,-4M80,60l14,8", stroke=GREY, w=1.2), p("M114,40l-6,4l6,4", stroke=DARK, w=1.6)],
 "single_glazing": [rect(0, 0, W, H, fill="#FFFFFF")] + window(20, 20, 50, 60) + [rect(90, 20, 50, 60, fill=SKY, stroke=LINE, w=2), rect(94, 24, 42, 52, fill="none", stroke=LINE, w=1),
   p("M45,90v6M115,90v6", stroke=GREY), p("M20,92h50M90,92h50", stroke=GREY), p("M36,96h18", stroke=RED, w=2), p("M104,96h22", stroke=GREEN, w=2)],
 "old_electrics": [rect(0, 0, W, H, fill="#FFFFFF"), rect(40, 14, 80, 80, fill="#ECEFF1", stroke=LINE, w=2), rect(54, 30, 52, 20, fill="#D7CCC8", stroke=LINE),
   circle(66, 40, 5, fill="#FFFFFF", stroke=LINE), circle(94, 40, 5, fill="#FFFFFF", stroke=LINE), p("M60,62q20,10 40,0M60,70q20,10 40,0", stroke=DARK, w=1.6),
   p("M80,94v14", stroke=DARK, w=2), p("M116,14l10,-8l-4,10l10,-6", stroke=RED, w=2)],
 "soil_against_wall": [rect(0, 0, W, H, fill="#FFFFFF"), rect(20, 8, 120, 92, fill=WALL, stroke=LINE), p("M0,110V58q40,-10 80,0t80,6V110z", fill="#8D6E63"),
   p("M20,64h120", stroke=BROWN, w=1, ), p("M28,52q10,-6 20,0M120,54q8,-6 16,0", stroke=GREEN, w=2), p("M30,74h100V86H30z", fill="#B9A27E", alpha="0.6")],
}

os.makedirs(OUT, exist_ok=True)
for name, parts in ILL.items():
    xml = ['<?xml version="1.0" encoding="utf-8"?>', "<!-- GENERATED by tools/art/gen_ill.py: schematic reference illustration, own artwork -->",
           f'<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="{W*2}dp" android:height="{H*2}dp" android:viewportWidth="{W}" android:viewportHeight="{H}">']
    xml += parts + ["</vector>"]
    with open(os.path.join(OUT, f"ill_{name}.xml"), "w") as f: f.write("\n".join(xml).replace(' android:fillColor="none"', "") + "\n")
print(len(ILL), "illustrations")
