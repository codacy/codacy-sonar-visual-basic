FROM mcr.microsoft.com/dotnet/core/sdk:2.2 as builder

COPY . /workdir
WORKDIR /workdir


RUN apt-get update &&\
    apt-get install -y apt-transport-https dirmngr gnupg ca-certificates

RUN apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 3FA7E0328081BFF6A14DA29AA6A19B38D3D831EF &&\
    echo "deb https://download.mono-project.com/repo/debian stable-stretch main" | tee /etc/apt/sources.list.d/mono-official-stable.list &&\
    apt-get update &&\
    apt-get install -y mono-complete build-essential nuget unzip libxml2-utils

RUN make &&\
    make documentation &&\
    make publish
  
FROM alpine:3.17.3

COPY --from=builder /workdir/src/Analyzer/bin/Release/net461/publish/*.dll /opt/docker/bin/
COPY --from=builder /workdir/src/Analyzer/bin/Release/net461/publish/*.exe /opt/docker/bin/
COPY --from=builder /workdir/docs /docs/

RUN apk add --no-cache mono --repository http://dl-cdn.alpinelinux.org/alpine/edge/testing &&\
    apk add --no-cache --virtual=.build-dependencies ca-certificates &&\
    cert-sync /etc/ssl/certs/ca-certificates.crt &&\
    apk del .build-dependencies &&\
    adduser -u 2004 -D docker &&\
    chown -R docker:docker /docs

ENTRYPOINT [ "mono", "/opt/docker/bin/Analyzer.exe" ]
