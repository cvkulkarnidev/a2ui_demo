package dev.cvkulkarnidev.a2ui

data class A2UIExample(val name: String, val description: String, val jsonl: String)

val A2UI_EXAMPLES = listOf(
    A2UIExample(
        "Component showcase",
        "Text, fields, checkbox, slider and actions",
        """{"version":"v0.9","createSurface":{"surfaceId":"showcase","catalogId":"basic","sendDataModel":true}}
{"version":"v0.9","updateDataModel":{"surfaceId":"showcase","value":{"title":"A2UI component showcase","subtitle":"Rendered natively with Jetpack Compose"}}}
{"version":"v0.9","updateComponents":{"surfaceId":"showcase","components":[{"id":"root","component":"Column","children":["title","subtitle","name","agree","range","submit"]},{"id":"title","component":"Text","text":{"path":"/title"},"variant":"h2"},{"id":"subtitle","component":"Text","text":{"path":"/subtitle"},"variant":"caption"},{"id":"name","component":"TextField","label":"Your name","value":"Chinmay","variant":"shortText"},{"id":"agree","component":"CheckBox","label":"Enable Android renderer","value":true},{"id":"range","component":"Slider","min":0,"max":10,"value":7},{"id":"submitLabel","component":"Text","text":"Send action"},{"id":"submit","component":"Button","child":"submitLabel","action":{"name":"submit_demo","context":{"source":"android"}}}]}}"""
    ),
    A2UIExample(
        "Flight booking",
        "Search for a one-way flight",
        """{"version":"v0.9","createSurface":{"surfaceId":"flight","catalogId":"basic","sendDataModel":true}}
{"version":"v0.9","updateDataModel":{"surfaceId":"flight","value":{"heading":"Book a flight","route":"Pune to Bengaluru","note":"Best fares for your next trip"}}}
{"version":"v0.9","updateComponents":{"surfaceId":"flight","components":[{"id":"root","component":"Column","children":["heading","note","routeCard","date","passengers","direct","search"]},{"id":"heading","component":"Text","text":{"path":"/heading"},"variant":"h2"},{"id":"note","component":"Text","text":{"path":"/note"},"variant":"caption"},{"id":"routeCard","component":"Card","child":"route"},{"id":"route","component":"Text","text":{"path":"/route"},"variant":"h3"},{"id":"date","component":"TextField","label":"Departure date","value":"20 Jul 2026","variant":"shortText"},{"id":"passengers","component":"Slider","min":1,"max":6,"value":2},{"id":"direct","component":"CheckBox","label":"Direct flights only","value":true},{"id":"searchLabel","component":"Text","text":"Search flights"},{"id":"search","component":"Button","child":"searchLabel","action":{"name":"search_flights","context":{"from":"PNQ","to":"BLR"}}}]}}"""
    ),
    A2UIExample(
        "Hotel booking",
        "Choose a stay and submit a search",
        """{"version":"v0.9","createSurface":{"surfaceId":"hotel","catalogId":"basic","sendDataModel":true}}
{"version":"v0.9","updateDataModel":{"surfaceId":"hotel","value":{"heading":"Find your stay","destination":"Goa","summary":"2 nights · 2 guests"}}}
{"version":"v0.9","updateComponents":{"surfaceId":"hotel","components":[{"id":"root","component":"Column","children":["heading","destination","summary","checkin","checkout","rooms","breakfast","search"]},{"id":"heading","component":"Text","text":{"path":"/heading"},"variant":"h2"},{"id":"destination","component":"TextField","label":"Destination","value":{"path":"/destination"},"variant":"shortText"},{"id":"summary","component":"Text","text":{"path":"/summary"},"variant":"caption"},{"id":"checkin","component":"TextField","label":"Check-in","value":"25 Jul 2026","variant":"shortText"},{"id":"checkout","component":"TextField","label":"Check-out","value":"27 Jul 2026","variant":"shortText"},{"id":"rooms","component":"Slider","min":1,"max":5,"value":1},{"id":"breakfast","component":"CheckBox","label":"Breakfast included","value":true},{"id":"searchLabel","component":"Text","text":"Search hotels"},{"id":"search","component":"Button","child":"searchLabel","action":{"name":"search_hotels","context":{"destination":"Goa"}}}]}}"""
    ),
    A2UIExample(
        "Food order",
        "A compact restaurant order form",
        """{"version":"v0.9","createSurface":{"surfaceId":"food","catalogId":"basic","sendDataModel":true}}
{"version":"v0.9","updateDataModel":{"surfaceId":"food","value":{"heading":"Order dinner","restaurant":"Green Bowl Kitchen","item":"Paneer tikka rice bowl","price":"₹329"}}}
{"version":"v0.9","updateComponents":{"surfaceId":"food","components":[{"id":"root","component":"Column","children":["heading","restaurantCard","item","price","quantity","cutlery","instructions","order"]},{"id":"heading","component":"Text","text":{"path":"/heading"},"variant":"h2"},{"id":"restaurantCard","component":"Card","child":"restaurant"},{"id":"restaurant","component":"Text","text":{"path":"/restaurant"},"variant":"h3"},{"id":"item","component":"Text","text":{"path":"/item"}},{"id":"price","component":"Text","text":{"path":"/price"},"variant":"h3"},{"id":"quantity","component":"Slider","min":1,"max":5,"value":1},{"id":"cutlery","component":"CheckBox","label":"Include cutlery","value":false},{"id":"instructions","component":"TextField","label":"Cooking instructions","value":"Less spicy","variant":"longText"},{"id":"orderLabel","component":"Text","text":"Place order"},{"id":"order","component":"Button","child":"orderLabel","action":{"name":"place_food_order","context":{"itemId":"paneer-bowl"}}}]}}"""
    ),
    A2UIExample(
        "User profile",
        "Editable profile and notification settings",
        """{"version":"v0.9","createSurface":{"surfaceId":"profile","catalogId":"basic","sendDataModel":true}}
{"version":"v0.9","updateDataModel":{"surfaceId":"profile","value":{"heading":"Your profile","name":"Chinmay Kulkarni","email":"cv.kulkarni.dev@gmail.com"}}}
{"version":"v0.9","updateComponents":{"surfaceId":"profile","components":[{"id":"root","component":"Column","children":["heading","name","email","bio","notifications","save"]},{"id":"heading","component":"Text","text":{"path":"/heading"},"variant":"h2"},{"id":"name","component":"TextField","label":"Full name","value":{"path":"/name"},"variant":"shortText"},{"id":"email","component":"TextField","label":"Email","value":{"path":"/email"},"variant":"shortText"},{"id":"bio","component":"TextField","label":"Bio","value":"Android and AI developer","variant":"longText"},{"id":"notifications","component":"CheckBox","label":"Product notifications","value":true},{"id":"saveLabel","component":"Text","text":"Save profile"},{"id":"save","component":"Button","child":"saveLabel","action":{"name":"save_profile"}}]}}"""
    ),
    A2UIExample(
        "Project dashboard",
        "A simple project status summary",
        """{"version":"v0.9","createSurface":{"surfaceId":"dashboard","catalogId":"basic","sendDataModel":false}}
{"version":"v0.9","updateDataModel":{"surfaceId":"dashboard","value":{"heading":"Project dashboard","project":"A2UI Android Renderer","status":"On track","progress":"Progress: 65%","next":"Next: API transport and streaming"}}}
{"version":"v0.9","updateComponents":{"surfaceId":"dashboard","components":[{"id":"root","component":"Column","children":["heading","projectCard","status","progress","slider","next","refresh"]},{"id":"heading","component":"Text","text":{"path":"/heading"},"variant":"h2"},{"id":"projectCard","component":"Card","child":"project"},{"id":"project","component":"Text","text":{"path":"/project"},"variant":"h3"},{"id":"status","component":"Text","text":{"path":"/status"}},{"id":"progress","component":"Text","text":{"path":"/progress"},"variant":"caption"},{"id":"slider","component":"Slider","min":0,"max":100,"value":65},{"id":"next","component":"Text","text":{"path":"/next"}},{"id":"refreshLabel","component":"Text","text":"Refresh dashboard"},{"id":"refresh","component":"Button","child":"refreshLabel","action":{"name":"refresh_dashboard"}}]}}"""
    )
)
