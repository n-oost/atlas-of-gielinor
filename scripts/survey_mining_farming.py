import urllib.request, json, time

endpoint = 'http://127.0.0.1:7780/mcp'
headers = {'Content-Type': 'application/json', 'Accept': 'application/json, text/event-stream'}

def call(tool, args=None):
    if args is None: args = {}
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

sites = [
    ('poi_005816', 3296, 8861, 0, 'Coral Farming Patch'),
    ('poi_005817', 1182, 10068, 0, 'Hespori Farming Patch'),
    ('poi_005818', 3730, 10271, 0, 'Seaweed Farming Patch'),
    ('poi_005819', 3614, 3856, 0, 'Spirit Tree Farming Patch'),
    ('poi_005820', 2788, 8781, 0, 'Lava Eel Fishing Spot'),
    ('poi_005825', 1847, 5414, 0, 'Ancient Cavern North Mining Site'),
    ('poi_005826', 1826, 5392, 0, 'Ancient Cavern South Mining Site'),
    ('poi_005827', 1840, 5397, 0, 'Ancient Cavern Middle Mining Site'),
    ('poi_005828', 1510, 9540, 0, 'Cam Torum Mining Site'),
    ('poi_005829', 2698, 8785, 0, 'Charred Dungeon Mining Site'),
    ('poi_005830', 3631, 3340, 0, 'Daeyalt Essence Mine'),
    ('poi_005831', 3299, 3021, 0, 'Desert Mining Camp Surface'),
    ('poi_005832', 2279, 9237, 0, 'Isle Of Souls Dungeon East'),
    ('poi_005833', 2262, 9244, 0, 'Isle Of Souls Dungeon West'),
    ('poi_005834', 3045, 10263, 0, 'Lava Maze Dungeon'),
    ('poi_005835', 2163, 10347, 0, 'Lunar Isle 1'),
    ('poi_005836', 2165, 10325, 0, 'Lunar Isle 2'),
    ('poi_005837', 2140, 10318, 0, 'Lunar Isle 3'),
    ('poi_005838', 2125, 10327, 0, 'Lunar Isle 4'),
    ('poi_005839', 2124, 10342, 0, 'Lunar Isle 5'),
    ('poi_005840', 1977, 9041, 0, 'Ogress Settlement Mining Site'),
    ('poi_005841', 3888, 9749, 0, 'Slepe Underground Mining Site'),
    ('poi_005842', 2221, 9003, 0, 'Sunbleak Cave North Mining Site'),
    ('poi_005843', 2224, 8977, 0, 'Sunbleak Cave South Mining Site'),
    ('poi_005844', 3295, 5987, 0, 'Trahearn Mining Site'),
    ('poi_005845', 3766, 3757, 0, 'Verdant Valley Mining Site')
]

for pid, x, y, plane, name in sites:
    call('type_chat', {'text': f'::tele {x} {y} {plane}'})
    time.sleep(1.0)
    state = call('get_client_state')
    px, py, pp = state.get("worldX"), state.get("worldY"), state.get("plane")
    objs = call('list_objects', {'radius': 15})
    if isinstance(objs, dict): objs = objs.get('objects', [])
    relevant = [o for o in objs if any(w in str(o.get('name', '')).lower() for w in ['rocks', 'rock', 'ore', 'patch', 'tree', 'hespori', 'seaweed', 'coral', 'essence', 'fishing'])]
    # Group rock/patch names
    counts = {}
    for o in relevant:
        n = o.get('name')
        counts[n] = counts.get(n, 0) + 1
    print(f"{pid} ({x},{y},{plane}) [{name}] -> arrived ({px},{py},{pp}): {counts}")
