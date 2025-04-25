# Running with JAR file
1. Download source
2. Download JAR [SatView.jar](SatView.jar)
3. Obtain your API key from [N2YO](https://www.n2yo.com/api/)
4. Copy `.env.example` into `.env` and paste API key there
5. `docker compose up -d`
6. Wait until python container finishes data calculation, you can see progress in its logs
7. Run JAR with `java -XstartOnFirstThread -jar SatView.jar`

# Running from source (not prepared yet)
1. Obtain your API key from [N2YO](https://www.n2yo.com/api/)
2. Copy `.env.example` into .env and paste API key there
2. `docker compose up -d`
3. Wait until python container finish data calculation, you can see progress in its logs
4. Run `src/Main.java`
5. MacOS version also needs to set VM option `-XstartOnFirstThread`



# Sources
- [Earth texture](https://visibleearth.nasa.gov/collection/1484/blue-marble)
- [Satellite info API](https://www.n2yo.com/api/)