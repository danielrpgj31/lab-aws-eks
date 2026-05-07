# Usando a imagem oficial do Node.js
FROM node:18

# Definindo o diretório de trabalho dentro do container
WORKDIR /usr/src/app

# Copiando o package.json e package-lock.json
COPY package*.json ./

# Instalando as dependências
RUN npm install

# Copiando o restante do código da aplicação e o arquivo proto
COPY . ./

# Expondo as portas que a aplicação irá utilizar
EXPOSE 8081
EXPOSE 50051

# Comando para iniciar a aplicação usando o registro do ts-node diretamente
CMD ["node", "-r", "ts-node/register", "-r", "./instrumentation.ts", "app.ts"]
