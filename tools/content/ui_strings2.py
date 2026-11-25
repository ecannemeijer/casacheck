# Collects all UI string modules named uis_*.py (key, nl, en, it).
import glob, importlib, os
S = []
for f in sorted(glob.glob(os.path.join(os.path.dirname(os.path.abspath(__file__)), "uis_*.py"))):
    S += importlib.import_module(os.path.basename(f)[:-3]).S
