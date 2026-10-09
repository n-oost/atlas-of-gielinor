import urllib.request, json, time

endpoint = 'http://127.0.0.1:7780/mcp'
headers = {'Content-Type': 'application/json', 'Accept': 'application/json, text/event-stream'}

def call(tool, args=None):
    if args is None:
        args = {}
    body = {'jsonrpc': '2.0', 'id': 1, 'method': 'tools/call', 'params': {'name': tool, 'arguments': args}}
    req = urllib.request.Request(endpoint, data=json.dumps(body).encode('utf-8'), headers=headers)
    with urllib.request.urlopen(req, timeout=10) as r:
        res = json.loads(r.read().decode())
    if res.get('isError'):
        raise RuntimeError(res)
    text = res['result']['content'][0]['text']
    return json.loads(text) if text.startswith('{') or text.startswith('[') else text

init = {'jsonrpc': '2.0', 'id': 1, 'method': 'initialize', 'params': {'protocolVersion': '2024-11-05', 'capabilities': {}, 'clientInfo': {'name': 'audit', 'version': '1.0'}}}
urllib.request.urlopen(urllib.request.Request(endpoint, data=json.dumps(init).encode('utf-8'), headers=headers), timeout=10)

targets = [
    ('poi_005846', 2675, 3275, 0, 'Ship to Brimhaven / Rimmington'),
    ('poi_005847', 1455, 2968, 0, 'Aldarin Charter Ship?'),
    ('poi_005848', 1743, 3136, 0, 'Fortis Cothon Charter Ship?'),
    ('poi_005849', 1943, 2753, 0, 'Deepfin Point Charter Ship?'),
    ('poi_005850', 2141, 3123, 0, 'Port Tyras Charter Ship?'),
    ('poi_005851', 2156, 3331, 0, 'Prifddinas Charter Ship?'),
    ('poi_005852', 3180, 6083, 0, 'Charter Ship (3180, 6083)?'),
    ('poi_005853', 3186, 2367, 0, 'The Great Conch Charter Ship?'),
    ('poi_005854', 1513, 2970, 0, 'Sunset Coast Charter Ship?'),
]

for pid, x, y, plane, desc in targets:
    print(f"=== Checking {pid}: ({x}, {y}, {plane}) - {desc} ===")
    call('type_chat', {'text': f'::tele {x} {y} {plane}'})
    time.sleep(1.2)
    state = call('get_client_state')
    px = state.get("worldX")
    py = state.get("worldY")
    pp = state.get("plane")
    print(f"Player at: {px}, {py}, {pp}")
    npcs = call('list_npcs', {'radius': 15})
    if isinstance(npcs, dict):
        npcs = npcs.get('npcs', [])
    print('NPCs:', [(n.get('id'), n.get('name'), n.get('worldX'), n.get('worldY')) for n in npcs[:10]])
    objs = call('list_objects', {'radius': 10})
    if isinstance(objs, dict):
        objs = objs.get('objects', [])
    ship_objs = [o for o in objs if any(w in str(o.get('name', '')).lower() for w in ['gangplank', 'ship', 'boat', 'plank', 'charter', 'board', 'ladder', 'anchor'])]
    print('Ship objects:', [(o.get('id'), o.get('name'), o.get('worldX'), o.get('worldY')) for o in ship_objs[:8]])
