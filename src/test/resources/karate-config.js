function fn() {
    const port = karate.properties['karate.port'] || 3000;
    return {
        baseUrl: 'http://localhost:' + port + '/api/v3',
    }
}