# Hosted submission component

Live host: https://cs5013-student-updates.sedateoats2.chatgpt.site

The desktop registers a classroom through Faculty tools. Share the classroom's
student link, not this general host address. No student account is required.

This directory retains the authored backend, form page, schema and API test for
review alongside the Java application. The complete buildable Vinext/Sites
checkout is managed in Sites project appgprj_6ae896a328cc8191992d796c9d5d4a04.
Its source commit for deployed version 2 is d9f0152e44bbcf3d5920bacaa63e19f6f1e320ce.

The backend derives a classroom ID from a random teacher key, accepts submissions
only for registered IDs, and requires the teacher key for downloads/removal.
Only IDs are registered by the desktop; local student names/titles are not sent.
Notes are online until imported or explicitly discarded. Rate limits, duplicate
IDs and origin validation apply to submissions. These controls do not authenticate
student identity; review is still necessary for a public submission link.

The Java desktop uses Windows DPAPI to store its teacher key. Do not commit or
share runtime preferences, response CSVs or database files. This source includes
no real student records or live teacher credentials.
