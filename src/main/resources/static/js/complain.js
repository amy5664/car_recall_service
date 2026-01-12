function check_ok(){
	const form = document.querySelector('.cmpl_frm');
				
				if(form.reporter_name.value.length==0){
					alert("이름을 작성해주세요.");
					form.reporter_name.focus();
					return;
				}
				if(form.password.value.length==0){
					alert("비밀번호를 작성해주세요.");
					form.password.focus();
					return;
				}
				if(form.password.value.length < 4){
					alert("비밀번호는 4글자 이상이여야 합니다.");
					form.password.focus();
					return;
				}
				if(form.phone.value.length==0){
			        alert("휴대폰 번호를 작성해주세요.");
					form.phone.focus();
					return;
				}
				if(form.title.value.length==0){
			        alert("제목을 작성해 주세요");
			        form.title.focus();
					return;
				}
				if(form.content.value.length==0){
			        alert("상담내용을 작성해 주세요");
			        form.title.focus();
					return;
				}
				form.submit();
			}
			