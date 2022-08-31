//import { S3Client } from "@aws-sdk/client-s3";
//const AWSXRay = require('aws-xray-sdk-core');
const AWS = require('aws-sdk');
const s3Client = new AWS.S3();

module.exports = {
    async listObjectFromS3(bucketName,prefix) {
        var params = { 
            Bucket: bucketName,
            Prefix: prefix
           }
           
            s3Client.listObjects(params, function (err, data) {
            if(err)throw err;
            console.log(data);
            return data;
           });
    }
}