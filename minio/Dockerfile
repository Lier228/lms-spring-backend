FROM golang:1.25-alpine AS server-build
RUN CGO_ENABLED=0 GOBIN=/out go install github.com/minio/minio@RELEASE.2025-10-15T17-29-55Z

FROM golang:1.25-alpine AS client-build
RUN CGO_ENABLED=0 GOBIN=/out go install github.com/minio/mc@RELEASE.2025-08-13T08-35-41Z

FROM alpine:3.22 AS minio
COPY --from=server-build /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/
COPY --from=server-build /out/minio /usr/local/bin/minio
RUN mkdir /data && chown 10001:10001 /data
USER 10001:10001
EXPOSE 9000 9001
ENTRYPOINT ["minio"]
CMD ["server", "/data", "--console-address", ":9001"]

FROM alpine:3.22 AS mc
COPY --from=client-build /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/
COPY --from=client-build /out/mc /usr/local/bin/mc
ENV MC_CONFIG_DIR=/tmp/mc
USER 10001:10001
ENTRYPOINT ["mc"]
