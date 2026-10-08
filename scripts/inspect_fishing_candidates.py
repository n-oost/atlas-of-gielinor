with open('src/main/resources/atlasofgielinor/catalog/pois.tsv', 'r', encoding='utf-8') as f:
    for line in f:
        p = line.strip('\r\n').split('\t')
        if len(p) > 0 and p[0] in [
            'poi_002661', 'poi_002664', 'poi_002665', # Sacred eel
            'poi_002829',                             # Crystal eel
            'poi_002735', 'poi_002738',               # Infernal eel
            'poi_002841', 'poi_002885',               # Camdozaal tetra
            'poi_003255', 'poi_003269', 'poi_003274', # Cave eel
            'poi_003026',                             # Shrimp/anchovies
            'poi_002649'                              # Volcanic ash/seaweed
        ]:
            desc_repr = repr(p[6]) if len(p) > 6 else ''
            print(f"{p[0]}\t{p[1]}\t{p[2]}\t{p[4]}\t{p[5]}\n   Desc: {desc_repr}\n")
