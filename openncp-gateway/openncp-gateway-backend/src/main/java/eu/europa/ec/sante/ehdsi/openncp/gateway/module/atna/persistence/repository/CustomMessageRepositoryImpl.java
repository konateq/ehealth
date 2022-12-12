package eu.europa.ec.sante.ehdsi.openncp.gateway.module.atna.persistence.repository;

import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.JPQLQuery;
import eu.europa.ec.sante.ehdsi.openncp.gateway.module.atna.persistence.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Objects;

@Repository
@Transactional(readOnly = true)
public class CustomMessageRepositoryImpl extends QuerydslRepositorySupport implements CustomMessageRepository {

    public CustomMessageRepositoryImpl() {
        super(MessageEntity.class);
    }

    @Override
    public Page<MessageEntity> findAllMessages(Pageable pageable) {

        QMessageEntity qMessage = QMessageEntity.messageEntity;
        JPQLQuery<Long> countQuery = from(qMessage)
                .select(qMessage.id);
        long totalElements =  countQuery.fetchCount();

        JPQLQuery<MessageEntity> fetchQuery = from(qMessage);

        List<MessageEntity> result = Objects.requireNonNull(getQuerydsl()).applyPagination(pageable, fetchQuery).fetch();
        return new PageImpl<>(result, pageable, totalElements);

    }

    @Override
    public Page<MessageEntity> searchMessages(Predicate predicate, Pageable pageable) {

        QMessageEntity qMessage = QMessageEntity.messageEntity;
        QMessageParticipantEntity qMessageParticipant = QMessageParticipantEntity.messageParticipantEntity;
        QParticipantEntity qParticipant = QParticipantEntity.participantEntity;
        QCode qCode = QCode.code;


        JPQLQuery<Long> idQuery = from(qMessage)
                .join(qMessage.messageParticipants, qMessageParticipant)
                .join(qMessageParticipant.participant, qParticipant)
                .join(qParticipant.participantTypes, qCode)
                .select(qMessage.id)
                .where(predicate);

        long totalElements =  idQuery.distinct().fetchCount();

        JPQLQuery<MessageEntity> messagesQuery =
                from(qMessage).where(qMessage.id.in(idQuery));

        List<MessageEntity> result = Objects.requireNonNull(getQuerydsl()).applyPagination(pageable, messagesQuery).fetch();
        return new PageImpl<>(result, pageable, totalElements);
    }

    @Override
    @Autowired
    public void setEntityManager(@Qualifier("atnaEntityManagerFactory") EntityManager entityManager) {
        super.setEntityManager(entityManager);
    }
}