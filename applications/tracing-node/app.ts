/*app.ts*/
import express, { Express } from 'express';
import * as grpc from '@grpc/grpc-js';
import * as protoLoader from '@grpc/proto-loader';
import path from 'path';
import os from 'os';

const PORT: number = parseInt(process.env.PORT || '8081');
const GRPC_PORT: number = parseInt(process.env.GRPC_PORT || '50051');
const HOSTNAME: string = os.hostname();
const app: Express = express();

// gRPC Setup
const PROTO_PATH = path.resolve(__dirname, 'proto', 'tracing.proto');
console.log(`Loading proto from: ${PROTO_PATH}`);
const packageDefinition = protoLoader.loadSync(PROTO_PATH, {
  keepCase: true,
  longs: String,
  enums: String,
  defaults: true,
  oneofs: true,
});
const tracingProto: any = grpc.loadPackageDefinition(packageDefinition).tracing;

const server = new grpc.Server({
  'grpc.max_receive_message_length': 1024 * 1024 * 4, // 4MB
  'grpc.max_send_message_length': 1024 * 1024 * 4,    // 4MB
  'grpc.keepalive_time_ms': 30000,
  'grpc.keepalive_timeout_ms': 10000,
  'grpc.keepalive_permit_without_calls': 1,
});

server.addService(tracingProto.TracingService.service, {
  getTrace: (call: any, callback: any) => {
    const name = call.request.name;
    console.log(`Received gRPC request for name: ${name} on pod: ${HOSTNAME}`);
    callback(null, { message: `Hello ${name} from gRPC Node.js [Pod: ${HOSTNAME}]` });
  },
});

server.bindAsync(`0.0.0.0:${GRPC_PORT}`, grpc.ServerCredentials.createInsecure(), (err, port) => {
  if (err) {
    console.error(`gRPC server failed to bind: ${err.message}`);
    return;
  }
  console.log(`gRPC Server listening on 0.0.0.0:${port}`);
});

// Keep Express for health checks or legacy REST
app.get('/health', (req, res) => {
  res.send('OK');
});

// Keep the old REST endpoint for reference or migration, but we'll focus on gRPC
app.get('/trace', (req, res) => {
  const name = req.query.name;
  console.log(`Received REST request for name: ${name} on pod: ${HOSTNAME}`);
  res.send(`Hello ${name} from REST Node.js [Pod: ${HOSTNAME}]`);
});

app.listen(PORT, () => {
  console.log(`Express Server listening on http://localhost:${PORT}`);
});
