# kaizo
Kaizo

The api:

Create Client Stream:
```
curl --request POST 'http://localhost:8080/streams' \
--header 'Content-Type: application/json' \
--data-raw '{

    "clientName": "clientName",
    "domain": "d3v-kaizo",
    "authInfo": "7bca63f5dbf2a6f2d2e5e9292682ccc4701cab07396c1ce30190c43908eb737d",    
    "system": {
        "ZenDesk": {}
    }

}'
```

Start Stream: `With StreamID`
```
curl --request PUT 'http://localhost:8080/streams/406f257a-3898-4451-87fc-1185b0ab5993/start' \
--header 'Content-Type: application/json' \
--data-raw '{
    "startFrom": "2019-01-01T00:00:00.000Z"
}'
```

Stop Stream
```
curl --request PUT 'http://localhost:8080/streams/406f257a-3898-4451-87fc-1185b0ab5993/stop'
```

Get All Streams
```
curl --request GET 'http://localhost:8080/streams'
```