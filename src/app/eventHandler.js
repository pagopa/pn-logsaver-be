//import { S3Client, ListObjectsV2Command } from "@aws-sdk/client-s3";
//import { PutItemCommand } from "@aws-sdk/client-dynamodb";
const s3ListObject = require('./s3ListObject.js')
//import { ddbClient } from "./ddbClient.js";
//import { s3Client } from "./s3Client.js";

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
        console.log(event)
        const parsingDate = new Date(event.time);
        
        
        let year = parsingDate.getFullYear();
        let month = parsingDate.getMonth();
        let day = parsingDate.getDay() - 1;
        let prefix = "logs/ecs/pnDelivery/" + year + "/" + month + "/" + day + "/";

        var result = await s3ListObject.listObjectFromS3( BUCKET_NAME, prefix );
        console.log(result);

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
