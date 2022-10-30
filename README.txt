/*
 * Trabalho entregue à disciplina de Fundamentos Teóricos da Computação
 * Alunos: Rafael Murta e Rossana Souza
 *
 * O programa AfnParaAfd é um código em java que transforma autômatos finitos não determinísticos em determinísticos.
 *
 * Para rodar o programa é possível utilizar o executável ou compilar o código localmente.
 *
 * javac AfnParaAfd.java --> para compilar
 * java AfnParaAfd --> para executar
 *
 * O programa faz a leitura de arquivo xml com extensão .jff de nome afn.jff e lê o autômato de entrada que deve estar na mesma pasta que o executavel.
 * Em seguida lê um arquivo de extensão .txt de nome sentencas.txt com as sentenças que serão testadas naquela máquina que também deve estar na mesma pasta que o executavel.
 * Escreve um arquivo com extensão .jff de nome afd.jff com o autômato determinístico de saída.
 * E por último, escreve num arquvo .txt de nome resultado.txt que informa se as sentenças testadas são ou não reconhecidas pelo AF..
 *
 * O arquivo com as sentenças de entrada não deve possuir linhas vazias no final
 */