
function selectProvincia() {
    var provinciaSelecionada = document.getElementById("provincia").value;
    // Selecione o elemento select na página
    var selectElement = document.getElementById("municipio");
    // Limpe as opções existentes no select
    selectElement.innerHTML = "";

    if (provinciaSelecionada !== "null") {
        
        var dados = {
            provinciaID: provinciaSelecionada
        };

        // Defina as opções da requisição
        var options = {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            }
        };

        // Especifique a URL da sua servlet
        var url = 'cadastrarCliente?provinciaId=' + provinciaSelecionada;

        // Faça a requisição POST usando fetch
        fetch(url, options)
                .then(function (response) {
                    // Verifique se a resposta foi bem-sucedida
                    if (response.ok) {
                        return response.text(); // Retorna a resposta como texto
                    } else {
                        throw new Error('Erro na requisição: ' + response.status);
                    }
                })
                .then(function (data) {
                    // Crie um elemento div temporário para armazenar a resposta como HTML
                    var tempDiv = document.createElement("div");



                    tempDiv.insertAdjacentHTML("beforeend", data);

                    // Obtenha todas as opções do elemento select da resposta
                    var options = tempDiv.querySelectorAll("#municipio option");

                    // Adicione as novas opções ao select
                    options.forEach(option => {

                        // Crie um novo elemento option
                        var newOption = document.createElement("option");
                        newOption.value = option.value;
                        newOption.textContent = option.textContent;

                        // Adicione o novo option ao select
                        selectElement.appendChild(newOption);
                    });
                })
                .catch(function (error) {
                    // Lógica de tratamento de erros
                    console.error(error);
                });
    }

}
