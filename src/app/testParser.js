const fs = require('fs');
const jokesFile = fs.readFileSync('../../jokes.json', 'utf8');

//console.log(jokesFile);

const jokes = JSON.parse(jokesFile);

console.log(jokes);