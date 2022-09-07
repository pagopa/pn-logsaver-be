const s3ListObject = require('./s3ListObject.js')
const s3GetObject = require('./s3GetObject.js')
var moment = require('moment');
const { ungzip } = require('node-gzip');

const BUCKET_NAME = process.env.BUCKET_NAME
const TABLE_NAME = process.env.TABLE_NAME

const bucketParams = {
    Bucket: BUCKET_NAME,
    Key: "KEY",
};

const params = {
    TableName: TABLE_NAME,
    Item: {
        CUSTOMER_ID: { N: "001" },
        CUSTOMER_NAME: { S: "Richard Roe" },
    },
};

const event_example = {
    "id": "cdc73f9d-aea9-11e3-9d5a-835b769c0d9c",
    "detail-type": "Scheduled Event",
    "source": "aws.events",
    "account": "123456789012",
    "time": "1970-01-01T00:00:00Z",
    "region": "us-east-1",
    "resources": [
        "arn:aws:events:us-east-1:123456789012:rule/ExampleRule"
    ],
    "detail": {}
}

module.exports = {
    async handleEvent(event){
        console.log(event.time); // 1970-01-01T00:00:00Z
        const parsingDate = moment.utc(event.time);
        console.log('parsingDate: '+ parsingDate.utc().format());
        const extractionDate = parsingDate.subtract(1, 'days').utc().format().split('T')[0];
        console.log('extractionDate: '+ extractionDate);
        var dateParts = extractionDate.split('-');
        console.log( 'year: '+ dateParts[0], 'month: '+dateParts[1], 'day: '+dateParts[2]  );
        
        
        let year = dateParts[0];
        
        let month = dateParts[1];
        
        let day = dateParts[2];
        
        //logs/ecs/pnDelivery/2022/07/11/12/pn-pnDelivery-ecs-delivery-stream-1-2022-07-11-12-56-15-53414149-a1c3-4053-bb1a-318423ee8ddf
        
        let prefix = "logs/ecs/pnDelivery/" + year + "/" + month + "/" + day + "/";
    
        console.log(prefix)
        
        try{
            var result = await s3ListObject.listObjectFromS3( BUCKET_NAME, prefix );
            //console.log('Result: ',result);
            var listKeys = [];
            for(var i=0; i < result.Contents.length;i++) {
                listKeys.push(result.Contents[i].Key);
            }
            console.log('listKeys', listKeys[0]);
            var result = await s3GetObject.getObjectFromS3( BUCKET_NAME, listKeys[0] );
            console.log('result', result);
            //var body = result.Body.toString('utf-8');
            //console.log('body', body);
            const unzipped = (await ungzip(result.Body)).toString();
            console.log('unzipped', unzipped);
            let js = JSON.parse( unzipped );
            console.log('parsed' ,js);

            //var decryptedBody = await s3Decrypt.decryptObject( body );
            
            //console.log('decryptedBody', decryptedBody);
        } catch(err) {
            console.log(err);
        }
        
        
        /*try {
            // Set the AWS Region.
            const REGION = "us-east-1";
            // Create an Amazon S3 service client object.
            const s3Client = new S3Client({ region: REGION });
            
            const streamToString = (stream) =>
            new Promise((resolve, reject) => {
                const chunks = [];
                stream.on("data", (chunk) => chunks.push(chunk));
                stream.on("error", reject);
                stream.on("end", () => resolve(Buffer.concat(chunks).toString("utf8")));
            });
            
            const data = await s3Client.send(new GetObjectCommand(bucketParams));
            const bodyContents = await streamToString(data.Body);
            console.log(bodyContents);
        } catch (err) {
            console.log("Error", err);
        } */
        
        // (logs|cdc) (pnDelivery|pnDeliveryPush|pnExternalRegistry|pnMandate|pnUserAttributes)
        // da inviare tramite detail nell'evento ricevuto
        
        let example_key = "logs/ecs/pnDelivery/2022/07/27/12/pn-pnDelivery-ecs-delivery-stream-1-2022-07-27-12-00-41-dd70d7b0-f319-46a7-a76c-4bcb1b698068"
        
        
        //const listObject = listObjectFromS3(prefix);
        //const stringFile = getFileFromS3(bucketParams);
        //const data = putItem(params);
    }
}

/*function putItem(params) {
    try {
        const data = await ddbClient.send(new PutItemCommand(params));
        console.log(data);
        return data;
    } catch (err) {
        console.error(err);
    }
}

function getFileFromS3(bucketParams) {
    try {
        // Set the AWS Region.
        const REGION = "us-east-1";
        // Create an Amazon S3 service client object.
        const s3Client = new S3Client({ region: REGION });
        
        const streamToString = (stream) =>
        new Promise((resolve, reject) => {
            const chunks = [];
            stream.on("data", (chunk) => chunks.push(chunk));
            stream.on("error", reject);
            stream.on("end", () => resolve(Buffer.concat(chunks).toString("utf8")));
        });
        
        const data = await s3Client.send(new GetObjectCommand(bucketParams));
        const bodyContents = await streamToString(data.Body);
        console.log(bodyContents);
        return bodyContents.promise();
    } catch (err) {
        console.log("Error", err);
    } 
}*/
