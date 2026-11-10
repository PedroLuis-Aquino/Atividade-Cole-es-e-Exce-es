# Cadastro de Pacientes (Java Swing)

Aplicação simples de cadastro de pacientes, feita em Java e Swing. A interface tem uma tela para adicionar pacientes e outra para listar os pacientes cadastrados. Os registros ficam em uma coleção durante a execução e são carregados/salvos no arquivo local `pacientes.txt`.

## Requisitos

- JDK instalado (Java 8 ou superior).
- Ambiente com interface gráfica para executar o Swing.

## Como executar

Na pasta do projeto, compile e execute:

```bash
javac Main.java
java Main
```

O arquivo `pacientes.txt` é criado na pasta de execução na primeira gravação. Os dados já existentes são carregados automaticamente ao iniciar. Cada campo é codificado em Base64 URL-safe para manter o formato do arquivo simples, inclusive quando os dados contêm vírgulas ou acentos.

## Validações e tratamento de erros

- Nome e diagnóstico são obrigatórios e possuem limites de tamanho.
- Idade deve estar entre 0 e 130 anos.
- Telefone deve conter de 7 a 20 dígitos e aceitar somente caracteres comuns de formatação.
- Entradas inválidas geram mensagens na interface; erros de leitura/gravação e registros corrompidos são tratados sem interromper a aplicação.
